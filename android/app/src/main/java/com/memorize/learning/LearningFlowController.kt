package com.memorize.learning

import android.content.Context
import com.memorize.database.MemorizeDatabase
import com.memorize.database.entity.*
import com.memorize.speech.SpeechRecognitionManager
import com.memorize.speech.TextToSpeechManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

enum class LearningPhase {
    PASS1,      // Bot reads, user repeats
    PASS2,      // User says first
    CUMULATIVE_REVIEW,  // Read from beginning to current position
    COMPLETED
}

class LearningFlowController(
    private val context: Context,
    private val database: MemorizeDatabase,
    private val textId: String,
    private val onComplete: (String) -> Unit
) {
    private val scope = CoroutineScope(Dispatchers.Main)
    private val pass1Controller = Pass1Controller(context, scope)
    private val pass2Controller = Pass2Controller(context, scope)
    private val ttsManager = TextToSpeechManager(context)
    private val speechRecognition = SpeechRecognitionManager(context)
    
    private var currentPhase = LearningPhase.PASS1
    private var currentSectionIndex = 0
    private var currentParagraphIndex = 0
    private var currentPhraseIndex = 0
    private var sections: List<SectionEntity> = emptyList()
    private var paragraphs: List<ParagraphEntity> = emptyList()
    private var phrases: List<PhraseEntity> = emptyList()
    private var sessionId: String? = null
    private var mistakesCount = 0
    private var repetitionsCount = 0
    private var startTime: Long = 0
    
    var onStateUpdate: ((LearningState) -> Unit)? = null
    
    suspend fun initialize(onStateUpdate: (LearningState) -> Unit) {
        this.onStateUpdate = onStateUpdate
        
        // Load text structure
        sections = database.sectionDao().getSectionsByTextId(textId).first()
        
        if (sections.isEmpty()) {
            onStateUpdate(LearningState())
            return
        }
        
        loadCurrentParagraph()
        
        // Initialize speech services
        pass1Controller.initialize()
        ttsManager.initialize { }
        speechRecognition.initialize()
        
        // Create learning session
        sessionId = UUID.randomUUID().toString()
        startTime = System.currentTimeMillis()
        val session = LearningSessionEntity(
            id = sessionId!!,
            textId = textId,
            startTime = startTime
        )
        database.learningSessionDao().insertSession(session)
        
        // Start with first phrase
        startPass1()
    }
    
    private suspend fun loadCurrentParagraph() {
        if (sections.isEmpty()) return
        
        val currentSection = sections[currentSectionIndex]
        paragraphs = database.paragraphDao().getParagraphsBySectionId(currentSection.id).first()
        
        if (paragraphs.isEmpty()) return
        
        val currentParagraph = paragraphs[currentParagraphIndex]
        phrases = database.phraseDao().getPhrasesByParagraphId(currentParagraph.id).first()
    }
    
    private suspend fun startPass1() {
        if (phrases.isEmpty()) {
            moveToNextParagraph()
            return
        }
        
        val currentPhrase = phrases[currentPhraseIndex]
        updateProgress()
        
        pass1Controller.execute(currentPhrase) { state ->
            onStateUpdate?.invoke(state.copy(
                currentSection = currentSectionIndex,
                totalSections = sections.size,
                currentParagraph = currentParagraphIndex,
                totalParagraphs = paragraphs.size,
                currentPhrase = currentPhraseIndex,
                totalPhrases = phrases.size
            ))
        }
    }
    
    fun onDontRemember(onStateUpdate: (LearningState) -> Unit) {
        scope.launch {
            this@LearningFlowController.onStateUpdate = onStateUpdate
            mistakesCount++
            repetitionsCount++
            
            val currentPhrase = phrases[currentPhraseIndex]
            onStateUpdate(LearningState(
                currentPhraseText = currentPhrase.text,
                isListening = false,
                feedback = "Вот правильная фраза. Повторите.",
                isCorrect = false,
                canContinue = false
            ))
            
            ttsManager.speak(currentPhrase.text)
        }
    }
    
    fun continueToNext(onStateUpdate: (LearningState) -> Unit) {
        scope.launch {
            this@LearningFlowController.onStateUpdate = onStateUpdate
            
            when (currentPhase) {
                LearningPhase.PASS1 -> {
                    // Mark phrase as learned in pass1
                    val currentPhrase = phrases[currentPhraseIndex]
                    database.phraseDao().updateLearnedStatus(currentPhrase.id, true)
                    
                    // Move to next phrase
                    currentPhraseIndex++
                    repetitionsCount++
                    
                    if (currentPhraseIndex >= phrases.size) {
                        // All phrases in paragraph learned in pass1, move to pass2
                        currentPhraseIndex = 0
                        currentPhase = LearningPhase.PASS2
                        startPass2()
                    } else {
                        startPass1()
                    }
                }
                LearningPhase.PASS2 -> {
                    // Mark phrase as learned in pass2
                    val currentPhrase = phrases[currentPhraseIndex]
                    database.phraseDao().updateLearnedStatus(currentPhrase.id, true)
                    
                    // Move to next phrase
                    currentPhraseIndex++
                    repetitionsCount++
                    
                    if (currentPhraseIndex >= phrases.size) {
                        // All phrases learned, move to cumulative review
                        currentPhraseIndex = 0
                        currentPhase = LearningPhase.CUMULATIVE_REVIEW
                        startCumulativeReview()
                    } else {
                        startPass2()
                    }
                }
                LearningPhase.CUMULATIVE_REVIEW -> {
                    // Continue cumulative review
                    continueCumulativeReview()
                }
                LearningPhase.COMPLETED -> {
                    // Already completed
                }
            }
        }
    }
    
    private suspend fun startPass2() {
        if (phrases.isEmpty()) {
            moveToNextParagraph()
            return
        }
        
        val currentPhrase = phrases[currentPhraseIndex]
        updateProgress()
        
        pass2Controller.execute(currentPhrase) { state ->
            onStateUpdate?.invoke(state.copy(
                currentSection = currentSectionIndex,
                totalSections = sections.size,
                currentParagraph = currentParagraphIndex,
                totalParagraphs = paragraphs.size,
                currentPhrase = currentPhraseIndex,
                totalPhrases = phrases.size
            ))
        }
    }
    
    private suspend fun startCumulativeReview() {
        // Ask user to read from beginning to current position
        val allPhrases = getAllPhrasesUpToCurrent()
        val fullText = allPhrases.joinToString(" ") { it.text }
        
        onStateUpdate?.invoke(LearningState(
            currentPhraseText = "",
            isListening = true,
            feedback = "Прочитайте весь текст с начала до текущего места",
            canContinue = false
        ))
        
        val userText = speechRecognition.recognizeSpeech()
        
        if (userText == null || TextComparator.isDontRemember(userText)) {
            // Help user
            onStateUpdate?.invoke(LearningState(
                currentPhraseText = fullText,
                isListening = false,
                feedback = "Вот текст. Слушайте внимательно.",
                canContinue = false
            ))
            
            ttsManager.speak(fullText)
            
            // Wait for user to repeat
            onStateUpdate?.invoke(LearningState(
                currentPhraseText = fullText,
                isListening = true,
                feedback = "Повторите весь текст",
                canContinue = false
            ))
            
            val repeatedText = speechRecognition.recognizeSpeech()
            
            if (repeatedText != null && TextComparator.compareTexts(fullText, repeatedText)) {
                onStateUpdate?.invoke(LearningState(
                    currentPhraseText = fullText,
                    isListening = false,
                    feedback = "Отлично! ✓",
                    isCorrect = true,
                    canContinue = true
                ))
            } else {
                mistakesCount++
                onStateUpdate?.invoke(LearningState(
                    currentPhraseText = fullText,
                    isListening = false,
                    feedback = "Попробуйте еще раз",
                    isCorrect = false,
                    canContinue = false
                ))
            }
        } else {
            val isCorrect = TextComparator.compareTexts(fullText, userText)
            
            if (isCorrect) {
                onStateUpdate?.invoke(LearningState(
                    currentPhraseText = fullText,
                    isListening = false,
                    feedback = "Превосходно! ✓",
                    isCorrect = true,
                    canContinue = true
                ))
            } else {
                mistakesCount++
                // Help user
                onStateUpdate?.invoke(LearningState(
                    currentPhraseText = fullText,
                    isListening = false,
                    feedback = "Не совсем правильно. Вот правильный текст.",
                    canContinue = false
                ))
                
                ttsManager.speak(fullText)
            }
        }
    }
    
    private suspend fun continueCumulativeReview() {
        // Move to next paragraph
        moveToNextParagraph()
    }
    
    private suspend fun moveToNextParagraph() {
        currentParagraphIndex++
        
        if (currentParagraphIndex >= paragraphs.size) {
            // Move to next section
            currentParagraphIndex = 0
            currentSectionIndex++
            
            if (currentSectionIndex >= sections.size) {
                // All sections completed
                completeLearning()
                return
            }
        }
        
        // Reset to pass1 for new paragraph
        currentPhraseIndex = 0
        currentPhase = LearningPhase.PASS1
        loadCurrentParagraph()
        startPass1()
    }
    
    private suspend fun getAllPhrasesUpToCurrent(): List<PhraseEntity> {
        val allPhrases = mutableListOf<PhraseEntity>()
        
        // Get all phrases from all previous sections
        for (i in 0 until currentSectionIndex) {
            val section = sections[i]
            val sectionParagraphs = database.paragraphDao().getParagraphsBySectionId(section.id).first()
            
            for (paragraph in sectionParagraphs) {
                val paragraphPhrases = database.phraseDao().getPhrasesByParagraphId(paragraph.id).first()
                allPhrases.addAll(paragraphPhrases)
            }
        }
        
        // Get phrases from current section up to current paragraph
        val currentSection = sections[currentSectionIndex]
        val currentSectionParagraphs = database.paragraphDao().getParagraphsBySectionId(currentSection.id).first()
        
        for (i in 0..currentParagraphIndex) {
            val paragraph = currentSectionParagraphs[i]
            val paragraphPhrases = database.phraseDao().getPhrasesByParagraphId(paragraph.id).first()
            
            if (i == currentParagraphIndex) {
                // Only add phrases up to current phrase index
                allPhrases.addAll(paragraphPhrases.take(currentPhraseIndex + 1))
            } else {
                allPhrases.addAll(paragraphPhrases)
            }
        }
        
        return allPhrases
    }
    
    private fun updateProgress() {
        onStateUpdate?.invoke(LearningState(
            currentSection = currentSectionIndex,
            totalSections = sections.size,
            currentParagraph = currentParagraphIndex,
            totalParagraphs = paragraphs.size,
            currentPhrase = currentPhraseIndex,
            totalPhrases = phrases.size
        ))
    }
    
    private suspend fun completeLearning() {
        currentPhase = LearningPhase.COMPLETED
        
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        
        // Calculate grade based on accuracy and time
        val accuracy = if (repetitionsCount > 0) {
            1.0 - (mistakesCount.toDouble() / repetitionsCount)
        } else {
            0.0
        }
        val grade = (accuracy * 100).toFloat()
        
        // Update session
        if (sessionId != null) {
            val session = database.learningSessionDao().getSessionById(sessionId!!)
            if (session != null) {
                val updatedSession = session.copy(
                    endTime = endTime,
                    totalRepetitions = repetitionsCount,
                    mistakesCount = mistakesCount,
                    grade = grade
                )
                database.learningSessionDao().updateSession(updatedSession)
            }
        }
        
        onComplete(sessionId ?: "")
    }
}

