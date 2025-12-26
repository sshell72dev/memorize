package com.memorize.learning

import android.content.Context
import com.memorize.database.entity.PhraseEntity
import com.memorize.speech.SpeechRecognitionManager
import com.memorize.speech.TextToSpeechManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class Pass1Controller(
    private val context: Context,
    private val scope: CoroutineScope,
    private val ttsManager: TextToSpeechManager // Use shared TTS instance
) {
    private val speechRecognition = SpeechRecognitionManager(context)
    private var isInitialized = false
    
    suspend fun initialize() {
        if (!isInitialized) {
            android.util.Log.d("Pass1Controller", "Initializing Pass1Controller")
            
            // TTS should already be initialized by LearningFlowController
            // Just initialize speech recognition
            val speechInitSuccess = speechRecognition.initialize()
            android.util.Log.d("Pass1Controller", "Speech recognition initialized: $speechInitSuccess")
            
            isInitialized = speechInitSuccess
        }
    }
    
    suspend fun execute(
        phrase: PhraseEntity,
        onStateUpdate: (LearningState) -> Unit
    ): Boolean {
        android.util.Log.d("Pass1Controller", "Starting execute for phrase: ${phrase.text}")
        
        // Step 1: Bot reads the phrase with typing animation
        onStateUpdate(LearningState(
            currentPhraseText = phrase.text,
            displayedText = "",
            isSpeaking = true,
            isListening = false,
            feedback = "Слушайте внимательно..."
        ))
        
        // Start TTS and typing animation synchronized
        var displayedChars = 0
        val totalChars = phrase.text.length
        
        // Estimate TTS duration (roughly 10-12 characters per second for Russian)
        val estimatedDurationMs = (totalChars / 11.0 * 1000).toLong()
        val charsPerUpdate = maxOf(1, totalChars / 100) // Update 100 times during speech
        val updateInterval = maxOf(10, estimatedDurationMs / 100)
        
        // Start TTS - this is a suspend function that waits for completion
        val typingJob = scope.launch {
            while (displayedChars < totalChars) {
                displayedChars = minOf(totalChars, displayedChars + charsPerUpdate)
                onStateUpdate(LearningState(
                    currentPhraseText = phrase.text,
                    displayedText = phrase.text.take(displayedChars),
                    isSpeaking = true,
                    isListening = false,
                    feedback = "Слушайте внимательно..."
                ))
                kotlinx.coroutines.delay(updateInterval)
            }
            // Ensure full text is displayed
            onStateUpdate(LearningState(
                currentPhraseText = phrase.text,
                displayedText = phrase.text,
                isSpeaking = true,
                isListening = false,
                feedback = "Слушайте внимательно..."
            ))
        }
        
        // Wait for TTS to complete - this will block until speech finishes
        android.util.Log.d("Pass1Controller", "Starting TTS for phrase: ${phrase.text}")
        val ttsSuccess = ttsManager.speak(phrase.text)
        android.util.Log.d("Pass1Controller", "TTS completed, success: $ttsSuccess")
        
        // Wait for typing animation to complete or timeout
        typingJob.join()
        android.util.Log.d("Pass1Controller", "Typing animation completed")
        
        // Update state to show TTS is done
        onStateUpdate(LearningState(
            currentPhraseText = phrase.text,
            displayedText = phrase.text,
            isSpeaking = false,
            isListening = false,
            feedback = "Готово. Повторите фразу",
            audioLevel = 0f
        ))
        
        // Small delay before starting microphone
        android.util.Log.d("Pass1Controller", "Waiting 500ms before starting microphone")
        kotlinx.coroutines.delay(500)
        
        // Step 2: Start listening with visualization
        var userSpokenText = ""
        var audioLevel = 0f
        
        // Update state to show we're ready to listen
        android.util.Log.d("Pass1Controller", "Updating state to listening mode")
        onStateUpdate(LearningState(
            currentPhraseText = phrase.text,
            displayedText = phrase.text,
            isSpeaking = false,
            isListening = true,
            feedback = "Повторите фразу",
            audioLevel = 0f
        ))
        
        // Use speech recognition with partial results
        val recognitionDeferred = kotlinx.coroutines.CompletableDeferred<String?>()
        
        // Launch recognition - SpeechRecognizer must be called on main thread
        android.util.Log.d("Pass1Controller", "Starting speech recognition")
        scope.launch(kotlinx.coroutines.Dispatchers.Main) {
            try {
                android.util.Log.d("Pass1Controller", "Calling recognizeSpeechWithProgress")
                speechRecognition.recognizeSpeechWithProgress(
                    onPartialResult = { partialText ->
                        android.util.Log.d("Pass1Controller", "Partial result: $partialText")
                        userSpokenText = partialText
                        // Simulate audio level based on text length (in real app, use actual audio level)
                        audioLevel = minOf(1f, partialText.length / 50f)
                        onStateUpdate(LearningState(
                            currentPhraseText = phrase.text,
                            displayedText = phrase.text,
                            userSpokenText = partialText,
                            isSpeaking = false,
                            isListening = true,
                            feedback = "Повторите фразу",
                            audioLevel = audioLevel
                        ))
                    },
                    onResult = { result ->
                        android.util.Log.d("Pass1Controller", "Recognition result: $result")
                        recognitionDeferred.complete(result)
                    }
                )
                android.util.Log.d("Pass1Controller", "recognizeSpeechWithProgress called successfully")
            } catch (e: Exception) {
                android.util.Log.e("Pass1Controller", "Error in speech recognition", e)
                e.printStackTrace()
                recognitionDeferred.complete(null)
            }
        }
        
        // Wait for recognition to complete
        android.util.Log.d("Pass1Controller", "Waiting for recognition to complete")
        val userText = recognitionDeferred.await()
        android.util.Log.d("Pass1Controller", "Recognition completed, userText: $userText")
        
        if (userText == null || TextComparator.isDontRemember(userText)) {
            // User doesn't remember or error occurred - repeat with voice and typing animation
            var errorDisplayedChars = 0
            val errorTotalChars = phrase.text.length
            val errorCharsPerUpdate = maxOf(1, errorTotalChars / 100)
            val errorEstimatedDurationMs = (errorTotalChars / 11.0 * 1000).toLong()
            val errorUpdateInterval = errorEstimatedDurationMs / 100
            
            onStateUpdate(LearningState(
                currentPhraseText = phrase.text,
                displayedText = "",
                isSpeaking = true,
                isListening = false,
                feedback = "Попробуйте еще раз. Вот правильная фраза:",
                isCorrect = false,
                canContinue = false,
                audioLevel = 0f
            ))
            
            // Start TTS for error case
            val errorTtsJob = scope.launch {
                ttsManager.speak(phrase.text)
            }
            
            // Typing animation for error case
            val errorTypingJob = scope.launch {
                while (errorDisplayedChars < errorTotalChars && errorTtsJob.isActive) {
                    errorDisplayedChars = minOf(errorTotalChars, errorDisplayedChars + errorCharsPerUpdate)
                    onStateUpdate(LearningState(
                        currentPhraseText = phrase.text,
                        displayedText = phrase.text.take(errorDisplayedChars),
                        isSpeaking = true,
                        isListening = false,
                        feedback = "Попробуйте еще раз. Вот правильная фраза:",
                        isCorrect = false,
                        canContinue = false,
                        audioLevel = 0f
                    ))
                    kotlinx.coroutines.delay(errorUpdateInterval)
                }
                if (errorDisplayedChars < errorTotalChars) {
                    onStateUpdate(LearningState(
                        currentPhraseText = phrase.text,
                        displayedText = phrase.text,
                        isSpeaking = true,
                        isListening = false,
                        feedback = "Попробуйте еще раз. Вот правильная фраза:",
                        isCorrect = false,
                        canContinue = false,
                        audioLevel = 0f
                    ))
                }
            }
            
            errorTtsJob.join()
            errorTypingJob.join()
            return false
        }
        
        // Step 3: Compare texts
        val isCorrect = TextComparator.compareTexts(phrase.text, userText)
        
        if (isCorrect) {
            onStateUpdate(LearningState(
                currentPhraseText = phrase.text,
                displayedText = phrase.text,
                userSpokenText = userText,
                isSpeaking = false,
                isListening = false,
                feedback = "Правильно! ✓",
                isCorrect = true,
                canContinue = true,
                audioLevel = 0f
            ))
            
            // Automatically continue after 1.5 seconds
            kotlinx.coroutines.delay(1500)
            // Signal that we can continue - this will be handled by LearningFlowController
            return true
        } else {
            onStateUpdate(LearningState(
                currentPhraseText = phrase.text,
                displayedText = phrase.text,
                userSpokenText = userText,
                isSpeaking = false,
                isListening = false,
                feedback = "Не совсем правильно. Вот правильная фраза:",
                isCorrect = false,
                canContinue = false,
                audioLevel = 0f
            ))
            
            // Repeat the phrase with typing animation
            var retryDisplayedChars = 0
            val retryTotalChars = phrase.text.length
            val retryCharsPerUpdate = maxOf(1, retryTotalChars / 100)
            val retryEstimatedDurationMs = (retryTotalChars / 11.0 * 1000).toLong()
            val retryUpdateInterval = retryEstimatedDurationMs / 100
            
            // Start TTS for retry
            val retryTtsJob = scope.launch {
                ttsManager.speak(phrase.text)
            }
            
            // Typing animation for retry
            val retryTypingJob = scope.launch {
                while (retryDisplayedChars < retryTotalChars && retryTtsJob.isActive) {
                    retryDisplayedChars = minOf(retryTotalChars, retryDisplayedChars + retryCharsPerUpdate)
                    onStateUpdate(LearningState(
                        currentPhraseText = phrase.text,
                        displayedText = phrase.text.take(retryDisplayedChars),
                        userSpokenText = userText,
                        isSpeaking = true,
                        isListening = false,
                        feedback = "Вот правильная фраза. Слушайте внимательно...",
                        isCorrect = false,
                        canContinue = false,
                        audioLevel = 0f
                    ))
                    kotlinx.coroutines.delay(retryUpdateInterval)
                }
                // Ensure full text is displayed
                if (retryDisplayedChars < retryTotalChars) {
                    onStateUpdate(LearningState(
                        currentPhraseText = phrase.text,
                        displayedText = phrase.text,
                        userSpokenText = userText,
                        isSpeaking = true,
                        isListening = false,
                        feedback = "Вот правильная фраза. Слушайте внимательно...",
                        isCorrect = false,
                        canContinue = false,
                        audioLevel = 0f
                    ))
                }
            }
            
            retryTtsJob.join()
            retryTypingJob.join()
            
            // Small delay before starting microphone
            kotlinx.coroutines.delay(300)
            
            // Retry recognition - automatically start listening
            onStateUpdate(LearningState(
                currentPhraseText = phrase.text,
                displayedText = phrase.text,
                userSpokenText = "",
                isSpeaking = false,
                isListening = true,
                feedback = "Попробуйте еще раз",
                isCorrect = false,
                canContinue = false,
                audioLevel = 0f
            ))
            
            val retryDeferred = kotlinx.coroutines.CompletableDeferred<String?>()
            
            speechRecognition.recognizeSpeechWithProgress(
                onPartialResult = { partialText ->
                    userSpokenText = partialText
                    audioLevel = minOf(1f, partialText.length / 50f)
                    onStateUpdate(LearningState(
                        currentPhraseText = phrase.text,
                        displayedText = phrase.text,
                        userSpokenText = partialText,
                        isSpeaking = false,
                        isListening = true,
                        feedback = "Попробуйте еще раз",
                        audioLevel = audioLevel
                    ))
                },
                onResult = { result ->
                    retryDeferred.complete(result)
                }
            )
            
            val retryText = retryDeferred.await()
            if (retryText != null && !TextComparator.isDontRemember(retryText)) {
                val retryIsCorrect = TextComparator.compareTexts(phrase.text, retryText)
                if (retryIsCorrect) {
                    onStateUpdate(LearningState(
                        currentPhraseText = phrase.text,
                        displayedText = phrase.text,
                        userSpokenText = retryText,
                        isSpeaking = false,
                        isListening = false,
                        feedback = "Правильно! ✓",
                        isCorrect = true,
                        canContinue = true,
                        audioLevel = 0f
                    ))
                    kotlinx.coroutines.delay(1500)
                    return true
                } else {
                    // Show feedback and allow another retry
                    onStateUpdate(LearningState(
                        currentPhraseText = phrase.text,
                        displayedText = phrase.text,
                        userSpokenText = retryText,
                        isSpeaking = false,
                        isListening = false,
                        feedback = "Не совсем правильно. Попробуйте еще раз.",
                        isCorrect = false,
                        canContinue = false,
                        audioLevel = 0f
                    ))
                    return false
                }
            } else {
                return false
            }
        }
        return false
    }
    
    fun release() {
        ttsManager.release()
        speechRecognition.release()
    }
}

