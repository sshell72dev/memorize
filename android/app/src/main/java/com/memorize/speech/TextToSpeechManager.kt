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
        android.util.Log.d("TextToSpeech", "Initializing TTS")
        tts = TextToSpeech(context) { status ->
            android.util.Log.d("TextToSpeech", "TTS initialization callback, status: $status")
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale("ru", "RU"))
                android.util.Log.d("TextToSpeech", "TTS language set result: $result")
                
                // Set volume to maximum
                tts?.setSpeechRate(1.0f) // Normal speed
                // Note: TTS volume is controlled by system volume, but we can ensure it's set correctly
                
                isReady = result != TextToSpeech.LANG_MISSING_DATA && 
                         result != TextToSpeech.LANG_NOT_SUPPORTED
                android.util.Log.d("TextToSpeech", "TTS isReady: $isReady")
                callback(isReady)
            } else {
                android.util.Log.e("TextToSpeech", "TTS initialization failed with status: $status")
                callback(false)
            }
        }
        
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                android.util.Log.d("TextToSpeech", "onStart: $utteranceId")
            }
            
            override fun onDone(utteranceId: String?) {
                android.util.Log.d("TextToSpeech", "onDone: $utteranceId")
            }
            
            override fun onError(utteranceId: String?) {
                android.util.Log.e("TextToSpeech", "onError: $utteranceId")
            }
        })
    }
    
    suspend fun speak(text: String): Boolean = suspendCancellableCoroutine { continuation ->
        android.util.Log.d("TextToSpeech", "speak called, isReady: $isReady, tts: $tts")
        if (!isReady || tts == null) {
            android.util.Log.e("TextToSpeech", "TTS not ready! isReady: $isReady, tts: $tts")
            continuation.resume(false)
            return@suspendCancellableCoroutine
        }
        
        val utteranceId = System.currentTimeMillis().toString()
        var isCompleted = false
        
        val listenerId = utteranceId
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            
            override fun onDone(utteranceId: String?) {
                if (!isCompleted && utteranceId == listenerId) {
                    isCompleted = true
                    continuation.resume(true)
                }
            }
            
            override fun onError(utteranceId: String?) {
                if (!isCompleted && utteranceId == listenerId) {
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
    
    fun speakWithProgress(
        text: String,
        onProgress: (Int) -> Unit
    ): Boolean {
        if (!isReady || tts == null) {
            return false
        }
        
        val utteranceId = System.currentTimeMillis().toString()
        val totalChars = text.length
        
        // Estimate speaking time (roughly 10 chars per second)
        val estimatedDuration = (totalChars / 10.0 * 1000).toLong()
        val charsPerInterval = maxOf(1, totalChars / 50) // Update 50 times
        
        var currentChar = 0
        
        val progressHandler = android.os.Handler(android.os.Looper.getMainLooper())
        val progressRunnable = object : Runnable {
            override fun run() {
                if (currentChar < totalChars) {
                    currentChar += charsPerInterval
                    onProgress(minOf(currentChar, totalChars))
                    progressHandler.postDelayed(this, estimatedDuration / 50)
                }
            }
        }
        
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                progressHandler.post(progressRunnable)
            }
            
            override fun onDone(utteranceId: String?) {
                progressHandler.removeCallbacks(progressRunnable)
                onProgress(totalChars)
            }
            
            override fun onError(utteranceId: String?) {
                progressHandler.removeCallbacks(progressRunnable)
            }
        })
        
        val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        return result != TextToSpeech.ERROR
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

