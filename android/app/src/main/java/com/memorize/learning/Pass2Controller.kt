package com.memorize.learning

import android.content.Context
import com.memorize.database.entity.PhraseEntity
import com.memorize.speech.SpeechRecognitionManager
import com.memorize.speech.TextToSpeechManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class Pass2Controller(
    private val context: Context,
    private val scope: CoroutineScope,
    private val ttsManager: TextToSpeechManager // Use shared TTS instance
) {
    private val speechRecognition = SpeechRecognitionManager(context)
    private var isInitialized = false
    private var userRequestedHelp = false
    
    suspend fun initialize() {
        if (!isInitialized) {
            android.util.Log.d("Pass2Controller", "Initializing Pass2Controller")
            val speechInitSuccess = speechRecognition.initialize()
            android.util.Log.d("Pass2Controller", "Speech recognition initialized: $speechInitSuccess")
            isInitialized = speechInitSuccess
        }
    }
    
    /**
     * Execute Pass2 for a phrase.
     * Returns true if phrase was completed without errors/hints, false otherwise.
     * 
     * Logic:
     * 1. User says phrase first
     * 2. If correct on first try -> return true (phrase passed without errors/hints)
     * 3. If error or "Не помню" -> bot helps (speaks + shows), user repeats, then cycle continues (user must say first again)
     * 4. Block is learned when all phrases pass without errors/hints
     */
    suspend fun execute(
        phrase: PhraseEntity,
        onStateUpdate: (LearningState) -> Unit,
        onDontRememberRequested: () -> Unit = {}
    ): Boolean {
        android.util.Log.d("Pass2Controller", "Starting Pass2 execute for phrase: ${phrase.text}")
        userRequestedHelp = false
        var hadHelp = false // Track if user received help at least once
        
        // Step A: Wait for user to say phrase first
        while (true) {
            onStateUpdate(LearningState(
                currentPhraseText = "", // Hide text when user should say first
                displayedText = "",
                isListening = true,
                isSpeaking = false,
                feedback = "Скажите фразу первым",
                isCorrect = false,
                canContinue = false,
                audioLevel = 0f
            ))
            
            // Start recognition with progress
            var userText: String? = null
            var recognitionCompleted = false
            
            speechRecognition.recognizeSpeechWithProgress(
                onPartialResult = { partialText ->
                    onStateUpdate(LearningState(
                        currentPhraseText = "",
                        displayedText = "",
                        userSpokenText = partialText,
                        isListening = true,
                        isSpeaking = false,
                        feedback = "Слушаю...",
                        audioLevel = 0.5f
                    ))
                },
                onResult = { result ->
                    userText = result
                    recognitionCompleted = true
                }
            )
            
            // Wait for recognition to complete
            while (!recognitionCompleted) {
                kotlinx.coroutines.delay(100)
                // Check if user requested help via button during recognition
                if (userRequestedHelp) {
                    android.util.Log.d("Pass2Controller", "User requested help via button during recognition")
                    userRequestedHelp = false
                    // Treat as error - will trigger help
                    recognitionCompleted = true
                    userText = null
                }
            }
            
            // Store in local variable to avoid smart cast issues
            val recognizedText = userText
            
            if (recognizedText == null || TextComparator.isDontRemember(recognizedText)) {
                // Error or "Не помню" - bot helps
                android.util.Log.d("Pass2Controller", "User said 'Не помню' or recognition failed")
                hadHelp = true
                val userRepeatedCorrectly = handleHelp(phrase, onStateUpdate)
                if (userRepeatedCorrectly) {
                    // User repeated correctly after help, continue cycle (user must say first again)
                    continue
                } else {
                    // User still has error after help, continue cycle (user can try again)
                    continue
                }
            } else {
                // Check if user said it correctly
                val isCorrect = TextComparator.compareTexts(phrase.text, recognizedText)
                
                if (isCorrect) {
                    // User said correctly
                    if (!hadHelp) {
                        // User said correctly on FIRST try - phrase passed without errors!
                        android.util.Log.d("Pass2Controller", "User said phrase correctly on first try - PASSED")
                        onStateUpdate(LearningState(
                            currentPhraseText = phrase.text,
                            displayedText = phrase.text,
                            userSpokenText = recognizedText,
                            isListening = false,
                            isSpeaking = false,
                            feedback = "Правильно! ✓",
                            isCorrect = true,
                            canContinue = true,
                            audioLevel = 0f
                        ))
                        kotlinx.coroutines.delay(1500) // Show success message
                        return true // Phrase passed without errors/hints
                    } else {
                        // User said correctly but had help before - phrase NOT passed without errors
                        android.util.Log.d("Pass2Controller", "User said phrase correctly but had help before - NOT PASSED")
                        onStateUpdate(LearningState(
                            currentPhraseText = phrase.text,
                            displayedText = phrase.text,
                            userSpokenText = recognizedText,
                            isListening = false,
                            isSpeaking = false,
                            feedback = "Правильно! Но была подсказка. Попробуйте еще раз с начала.",
                            isCorrect = true,
                            canContinue = false,
                            audioLevel = 0f
                        ))
                        kotlinx.coroutines.delay(2000) // Show message
                        // Reset and try again
                        hadHelp = false
                        continue
                    }
                } else {
                    // User made mistake - bot helps
                    android.util.Log.d("Pass2Controller", "User made mistake, bot will help")
                    hadHelp = true
                    val userRepeatedCorrectly = handleHelp(phrase, onStateUpdate)
                    if (userRepeatedCorrectly) {
                        // User repeated correctly after help, continue cycle
                        continue
                    } else {
                        // User still has error after help, continue cycle
                        continue
                    }
                }
            }
        }
    }
    
    /**
     * Handle help: bot speaks and shows text, user repeats.
     * Returns true if user repeated correctly, false if still error.
     */
    private suspend fun handleHelp(
        phrase: PhraseEntity,
        onStateUpdate: (LearningState) -> Unit
    ): Boolean {
        android.util.Log.d("Pass2Controller", "Bot helping with phrase: ${phrase.text}")
        
        // Bot shows and speaks the phrase
        onStateUpdate(LearningState(
            currentPhraseText = phrase.text,
            displayedText = phrase.text,
            isListening = false,
            isSpeaking = true,
            feedback = "Вот правильная фраза. Слушайте внимательно...",
            audioLevel = 0f
        ))
        
        // Speak the phrase
        val ttsSuccess = ttsManager.speak(phrase.text)
        android.util.Log.d("Pass2Controller", "TTS completed, success: $ttsSuccess")
        
        // Small delay after TTS
        kotlinx.coroutines.delay(500)
        
        // Wait for user to repeat
        onStateUpdate(LearningState(
            currentPhraseText = phrase.text,
            displayedText = phrase.text,
            isListening = true,
            isSpeaking = false,
            feedback = "Повторите фразу",
            audioLevel = 0f
        ))
        
        var repeatedText: String? = null
        var recognitionCompleted = false
        
        speechRecognition.recognizeSpeechWithProgress(
            onPartialResult = { partialText ->
                onStateUpdate(LearningState(
                    currentPhraseText = phrase.text,
                    displayedText = phrase.text,
                    userSpokenText = partialText,
                    isListening = true,
                    isSpeaking = false,
                    feedback = "Повторите фразу",
                    audioLevel = 0.5f
                ))
            },
            onResult = { result ->
                repeatedText = result
                recognitionCompleted = true
            }
        )
        
        // Wait for recognition to complete
        while (!recognitionCompleted) {
            kotlinx.coroutines.delay(100)
        }
        
        // Store in local variable to avoid smart cast issues
        val recognizedRepeatedText = repeatedText
        
        if (recognizedRepeatedText != null && TextComparator.compareTexts(phrase.text, recognizedRepeatedText)) {
            // User repeated correctly
            android.util.Log.d("Pass2Controller", "User repeated correctly after help")
            onStateUpdate(LearningState(
                currentPhraseText = phrase.text,
                displayedText = phrase.text,
                userSpokenText = recognizedRepeatedText,
                isListening = false,
                isSpeaking = false,
                feedback = "Правильно! Теперь скажите фразу первым",
                isCorrect = true,
                canContinue = false,
                audioLevel = 0f
            ))
            kotlinx.coroutines.delay(1500) // Show message
            return true // User repeated correctly
        } else {
            // Still error
            android.util.Log.d("Pass2Controller", "User still has error after help")
            onStateUpdate(LearningState(
                currentPhraseText = phrase.text,
                displayedText = phrase.text,
                isListening = false,
                isSpeaking = false,
                feedback = "Попробуйте еще раз. Скажите фразу первым.",
                isCorrect = false,
                canContinue = false,
                audioLevel = 0f
            ))
            kotlinx.coroutines.delay(1500)
            return false // Still has error
        }
    }
    
    /**
     * Called when user presses "Не помню" button
     */
    fun requestHelp() {
        android.util.Log.d("Pass2Controller", "Help requested via button")
        userRequestedHelp = true
    }
    
    fun release() {
        speechRecognition.release()
        // Don't release TTS as it's shared
    }
}

