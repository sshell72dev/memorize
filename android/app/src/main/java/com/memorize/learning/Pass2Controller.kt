package com.memorize.learning

import android.content.Context
import com.memorize.database.entity.PhraseEntity
import com.memorize.speech.SpeechRecognitionManager
import com.memorize.speech.TextToSpeechManager
import kotlinx.coroutines.CoroutineScope

class Pass2Controller(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val ttsManager = TextToSpeechManager(context)
    private val speechRecognition = SpeechRecognitionManager(context)
    
    suspend fun execute(
        phrase: PhraseEntity,
        onStateUpdate: (LearningState) -> Unit
    ) {
        // User says first
        onStateUpdate(LearningState(
            currentPhraseText = "",
            isListening = true,
            feedback = "Скажите фразу"
        ))
        
        val userText = speechRecognition.recognizeSpeech()
        
        if (userText == null || TextComparator.isDontRemember(userText)) {
            // Bot helps
            onStateUpdate(LearningState(
                currentPhraseText = phrase.text,
                isListening = false,
                feedback = "Вот правильная фраза. Повторите."
            ))
            
            ttsManager.speak(phrase.text)
            
            // Wait for user to repeat
            onStateUpdate(LearningState(
                currentPhraseText = phrase.text,
                isListening = true,
                feedback = "Повторите фразу"
            ))
            
            val repeatedText = speechRecognition.recognizeSpeech()
            
            if (repeatedText != null && TextComparator.compareTexts(phrase.text, repeatedText)) {
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
                    feedback = "Попробуйте еще раз",
                    isCorrect = false,
                    canContinue = false
                ))
            }
        } else {
            // Check if user said it correctly
            val isCorrect = TextComparator.compareTexts(phrase.text, userText)
            
            if (isCorrect) {
                onStateUpdate(LearningState(
                    currentPhraseText = phrase.text,
                    isListening = false,
                    feedback = "Отлично! ✓",
                    isCorrect = true,
                    canContinue = true
                ))
            } else {
                // Bot helps
                onStateUpdate(LearningState(
                    currentPhraseText = phrase.text,
                    isListening = false,
                    feedback = "Не совсем правильно. Вот правильная фраза."
                ))
                
                ttsManager.speak(phrase.text)
                
                // Wait for user to repeat
                onStateUpdate(LearningState(
                    currentPhraseText = phrase.text,
                    isListening = true,
                    feedback = "Повторите фразу"
                ))
                
                val repeatedText = speechRecognition.recognizeSpeech()
                
                if (repeatedText != null && TextComparator.compareTexts(phrase.text, repeatedText)) {
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
                        feedback = "Попробуйте еще раз",
                        isCorrect = false,
                        canContinue = false
                    ))
                }
            }
        }
    }
    
    fun release() {
        ttsManager.release()
        speechRecognition.release()
    }
}

