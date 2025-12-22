package com.memorize.learning

import android.content.Context
import com.memorize.database.entity.PhraseEntity
import com.memorize.speech.SpeechRecognitionManager
import com.memorize.speech.TextToSpeechManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class Pass1Controller(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val ttsManager = TextToSpeechManager(context)
    private val speechRecognition = SpeechRecognitionManager(context)
    private var isInitialized = false
    
    suspend fun initialize() {
        if (!isInitialized) {
            ttsManager.initialize { success ->
                isInitialized = success
            }
            speechRecognition.initialize()
            isInitialized = true
        }
    }
    
    suspend fun execute(
        phrase: PhraseEntity,
        onStateUpdate: (LearningState) -> Unit
    ) {
        // Bot reads the phrase
        onStateUpdate(LearningState(
            currentPhraseText = phrase.text,
            isListening = false,
            feedback = "Слушайте внимательно..."
        ))
        
        ttsManager.speak(phrase.text)
        
        // Wait for user to repeat
        onStateUpdate(LearningState(
            currentPhraseText = phrase.text,
            isListening = true,
            feedback = "Повторите фразу"
        ))
        
        val userText = speechRecognition.recognizeSpeech()
        
        if (userText == null || TextComparator.isDontRemember(userText)) {
            // User doesn't remember or error occurred
            onStateUpdate(LearningState(
                currentPhraseText = phrase.text,
                isListening = false,
                feedback = "Попробуйте еще раз",
                isCorrect = false,
                canContinue = false
            ))
            return
        }
        
        val isCorrect = TextComparator.compareTexts(phrase.text, userText)
        
        if (isCorrect) {
            onStateUpdate(LearningState(
                currentPhraseText = phrase.text,
                isListening = false,
                feedback = "Правильно! ✓",
                isCorrect = true,
                canContinue = true
            ))
        } else {
            onStateUpdate(LearningState(
                currentPhraseText = phrase.text,
                isListening = false,
                feedback = "Не совсем правильно. Попробуйте еще раз.",
                isCorrect = false,
                canContinue = false
            ))
        }
    }
    
    fun release() {
        ttsManager.release()
        speechRecognition.release()
    }
}

