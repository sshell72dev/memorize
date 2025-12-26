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
    private val ttsManager = TextToSpeechManager(context)
    private val speechRecognition = SpeechRecognitionManager(context)
    private val pass1Controller = Pass1Controller(context, scope, ttsManager) // Pass shared TTS
    private val pass2Controller = Pass2Controller(context, scope, ttsManager) // Pass shared TTS
    
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
        
        // Initialize speech services - wait for TTS to be ready
        android.util.Log.d("LearningFlowController", "Initializing speech services")
        
        // Initialize TTS first and wait for completion
        val ttsInitDeferred = kotlinx.coroutines.CompletableDeferred<Boolean>()
        ttsManager.initialize { success ->
            android.util.Log.d("LearningFlowController", "TTS manager initialized: $success")
            ttsInitDeferred.complete(success)
        }
        val ttsInitSuccess = ttsInitDeferred.await()
        android.util.Log.d("LearningFlowController", "TTS initialization completed: $ttsInitSuccess")
        
        // Initialize speech recognition
        val speechInitSuccess = speechRecognition.initialize()
        android.util.Log.d("LearningFlowController", "Speech recognition initialized: $speechInitSuccess")
        
        // Initialize Pass1Controller and Pass2Controller (which will use already initialized services)
        pass1Controller.initialize()
        pass2Controller.initialize()
        
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
        android.util.Log.d("LearningFlowController", "Starting first phrase")
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
        
        val isCorrect = pass1Controller.execute(currentPhrase) { state ->
            val updatedState = state.copy(
                currentSection = currentSectionIndex,
                totalSections = sections.size,
                currentParagraph = currentParagraphIndex,
                totalParagraphs = paragraphs.size,
                currentPhrase = currentPhraseIndex,
                totalPhrases = phrases.size,
                currentPhase = LearningPhase.PASS1
            )
            onStateUpdate?.invoke(updatedState)
        }
        
        // Auto-continue to next phrase if correct
        if (isCorrect) {
            kotlinx.coroutines.delay(500) // Small delay before auto-continue
            continueToNextInternal()
        }
    }
    
    fun onDontRemember(onStateUpdate: (LearningState) -> Unit) {
        scope.launch {
            this@LearningFlowController.onStateUpdate = onStateUpdate
            
            when (currentPhase) {
                LearningPhase.PASS1 -> {
                    mistakesCount++
                    repetitionsCount++
                    
                    val currentPhrase = phrases[currentPhraseIndex]
                    onStateUpdate(LearningState(
                        currentPhraseText = currentPhrase.text,
                        isListening = false,
                        feedback = "Вот правильная фраза. Повторите.",
                        isCorrect = false,
                        canContinue = false,
                        currentPhase = LearningPhase.PASS1
                    ))
                    
                    android.util.Log.d("LearningFlowController", "onDontRemember (PASS1): speaking phrase: ${currentPhrase.text}")
                    val ttsSuccess = ttsManager.speak(currentPhrase.text)
                    android.util.Log.d("LearningFlowController", "onDontRemember (PASS1): TTS completed, success: $ttsSuccess")
                }
                LearningPhase.PASS2 -> {
                    // In Pass2, "Не помню" triggers help
                    android.util.Log.d("LearningFlowController", "onDontRemember (PASS2): requesting help")
                    pass2Controller.requestHelp()
                }
                else -> {
                    // Not applicable in other phases
                }
            }
        }
    }
    
    fun continueToNext(onStateUpdate: (LearningState) -> Unit) {
        scope.launch {
            this@LearningFlowController.onStateUpdate = onStateUpdate
            continueToNextInternal()
        }
    }
    
    private suspend fun continueToNextInternal() {
        when (currentPhase) {
            LearningPhase.PASS1 -> {
                // Mark phrase as learned in pass1
                val currentPhrase = phrases[currentPhraseIndex]
                database.phraseDao().updateLearnedStatus(currentPhrase.id, true)
                
                // Move to next phrase
                currentPhraseIndex++
                repetitionsCount++
                
                if (currentPhraseIndex >= phrases.size) {
                    // All phrases in paragraph learned in pass1, show instruction before pass2
                    android.util.Log.d("LearningFlowController", "All phrases in paragraph completed in PASS1, showing Pass2 instruction")
                    currentPhraseIndex = 0
                    currentPhase = LearningPhase.PASS2
                    showPass2Instruction()
                } else {
                    startPass1()
                }
            }
            LearningPhase.PASS2 -> {
                // In Pass2, phrase is only marked as learned if it passed without errors
                // (This is handled in startPass2 based on return value)
                // Move to next phrase
                currentPhraseIndex++
                repetitionsCount++
                
                if (currentPhraseIndex >= phrases.size) {
                    // All phrases in paragraph passed Pass2 without errors
                    android.util.Log.d("LearningFlowController", "All phrases in paragraph completed in PASS2, moving to next paragraph")
                    moveToNextParagraph()
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
    
    private suspend fun showPass2Instruction() {
        if (phrases.isEmpty()) {
            moveToNextParagraph()
            return
        }
        
        // Collect all phrases text for the instruction
        val fullText = phrases.joinToString(" ") { it.text }
        
        android.util.Log.d("LearningFlowController", "Showing Pass2 instruction with text length: ${fullText.length}")
        
        onStateUpdate?.invoke(LearningState(
            currentSection = currentSectionIndex,
            totalSections = sections.size,
            currentParagraph = currentParagraphIndex,
            totalParagraphs = paragraphs.size,
            currentPhrase = 0,
            totalPhrases = phrases.size,
            currentPhase = LearningPhase.PASS2,
            showPass2Instruction = true,
            pass2InstructionText = fullText
        ))
    }
    
    fun startPass2AfterInstruction(onStateUpdate: (LearningState) -> Unit) {
        scope.launch {
            this@LearningFlowController.onStateUpdate = onStateUpdate
            startPass2()
        }
    }
    
    private suspend fun startPass2() {
        if (phrases.isEmpty()) {
            moveToNextParagraph()
            return
        }
        
        // Hide instruction screen
        onStateUpdate?.invoke(LearningState(
            currentSection = currentSectionIndex,
            totalSections = sections.size,
            currentParagraph = currentParagraphIndex,
            totalParagraphs = paragraphs.size,
            currentPhrase = currentPhraseIndex,
            totalPhrases = phrases.size,
            currentPhase = LearningPhase.PASS2,
            showPass2Instruction = false,
            pass2InstructionText = ""
        ))
        
        val currentPhrase = phrases[currentPhraseIndex]
        updateProgress()
        
        // Execute Pass2 - returns true if phrase passed without errors/hints
        val passedWithoutErrors = pass2Controller.execute(
            currentPhrase,
            onStateUpdate = { state ->
                onStateUpdate?.invoke(state.copy(
                    currentSection = currentSectionIndex,
                    totalSections = sections.size,
                    currentParagraph = currentParagraphIndex,
                    totalParagraphs = paragraphs.size,
                    currentPhrase = currentPhraseIndex,
                    totalPhrases = phrases.size,
                    currentPhase = LearningPhase.PASS2
                ))
            },
            onDontRememberRequested = {
                // This is handled via requestHelp() method
            }
        )
        
        if (passedWithoutErrors) {
            // Phrase passed without errors - mark as learned
            android.util.Log.d("LearningFlowController", "Phrase passed Pass2 without errors: ${currentPhrase.text}")
            database.phraseDao().updateLearnedStatus(currentPhrase.id, true)
            // Auto-continue to next phrase
            kotlinx.coroutines.delay(500)
            continueToNextInternal()
        } else {
            // Phrase had errors - user needs to try again
            android.util.Log.d("LearningFlowController", "Phrase had errors in Pass2, user will try again: ${currentPhrase.text}")
            mistakesCount++
            // Don't mark as learned, don't auto-continue
            // User will need to complete the phrase without errors
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
            totalPhrases = phrases.size,
            currentPhase = currentPhase
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

