package com.memorize.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume

class TextToSpeechManager(private val context: Context) {
    private var tts: TextToSpeech? = null
    private var isReady = false
    
    fun initialize(callback: (Boolean) -> Unit) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale("ru", "RU"))
                isReady = result != TextToSpeech.LANG_MISSING_DATA && 
                         result != TextToSpeech.LANG_NOT_SUPPORTED
                callback(isReady)
            } else {
                callback(false)
            }
        }
        
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                // Called when speech starts
            }
            
            override fun onDone(utteranceId: String?) {
                // Called when speech finishes
            }
            
            override fun onError(utteranceId: String?) {
                // Called on error
            }
        })
    }
    
    suspend fun speak(text: String): Boolean = suspendCancellableCoroutine { continuation ->
        if (!isReady || tts == null) {
            continuation.resume(false)
            return@suspendCancellableCoroutine
        }
        
        val utteranceId = System.currentTimeMillis().toString()
        var isCompleted = false
        
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            
            override fun onDone(utteranceId: String?) {
                if (!isCompleted && utteranceId == utteranceId) {
                    isCompleted = true
                    continuation.resume(true)
                }
            }
            
            override fun onError(utteranceId: String?) {
                if (!isCompleted && utteranceId == utteranceId) {
                    isCompleted = true
                    continuation.resume(false)
                }
            }
        })
        
        val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        if (result == TextToSpeech.ERROR) {
            continuation.resume(false)
        }
    }
    
    fun stop() {
        tts?.stop()
    }
    
    fun setSpeechRate(rate: Float) {
        tts?.setSpeechRate(rate)
    }
    
    fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady = false
    }
}

