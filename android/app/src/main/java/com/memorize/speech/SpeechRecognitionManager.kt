package com.memorize.speech

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class SpeechRecognitionManager(private val context: Context) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    
    fun initialize(): Boolean {
        android.util.Log.d("SpeechRecognition", "Initializing SpeechRecognizer")
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            android.util.Log.e("SpeechRecognition", "Speech recognition is not available on this device")
            return false
        }
        
        android.util.Log.d("SpeechRecognition", "Speech recognition is available, creating recognizer")
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        val success = speechRecognizer != null
        android.util.Log.d("SpeechRecognition", "SpeechRecognizer created: $success")
        return success
    }
    
    suspend fun recognizeSpeech(): String? = suspendCancellableCoroutine { continuation ->
        if (speechRecognizer == null || isListening) {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }
        
        isListening = true
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            
            override fun onBeginningOfSpeech() {}
            
            override fun onRmsChanged(rmsdB: Float) {}
            
            override fun onBufferReceived(buffer: ByteArray?) {}
            
            override fun onEndOfSpeech() {}
            
            override fun onError(error: Int) {
                isListening = false
                continuation.resume(null)
            }
            
            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull()
                continuation.resume(text)
            }
            
            override fun onPartialResults(partialResults: Bundle?) {}
            
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        
        speechRecognizer?.startListening(intent)
    }
    
    fun recognizeSpeechWithProgress(
        onPartialResult: (String) -> Unit,
        onResult: (String?) -> Unit
    ) {
        android.util.Log.d("SpeechRecognition", "recognizeSpeechWithProgress called, speechRecognizer: $speechRecognizer, isListening: $isListening")
        
        // Check permissions
        val hasPermission = PackageManager.PERMISSION_GRANTED == 
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO)
        android.util.Log.d("SpeechRecognition", "Microphone permission: $hasPermission")
        
        if (!hasPermission) {
            android.util.Log.e("SpeechRecognition", "Microphone permission not granted!")
            onResult(null)
            return
        }
        
        if (speechRecognizer == null || isListening) {
            android.util.Log.w("SpeechRecognition", "Cannot start recognition: speechRecognizer is null or already listening")
            onResult(null)
            return
        }
        
        isListening = true
        android.util.Log.d("SpeechRecognition", "Starting speech recognition")
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                android.util.Log.d("SpeechRecognition", "onReadyForSpeech")
            }
            
            override fun onBeginningOfSpeech() {
                android.util.Log.d("SpeechRecognition", "onBeginningOfSpeech")
            }
            
            override fun onRmsChanged(rmsdB: Float) {
                // Log occasionally to avoid spam
            }
            
            override fun onBufferReceived(buffer: ByteArray?) {
                android.util.Log.d("SpeechRecognition", "onBufferReceived")
            }
            
            override fun onEndOfSpeech() {
                android.util.Log.d("SpeechRecognition", "onEndOfSpeech")
            }
            
            override fun onError(error: Int) {
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "ERROR_AUDIO"
                    SpeechRecognizer.ERROR_CLIENT -> "ERROR_CLIENT"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "ERROR_INSUFFICIENT_PERMISSIONS"
                    SpeechRecognizer.ERROR_NETWORK -> "ERROR_NETWORK"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "ERROR_NETWORK_TIMEOUT"
                    SpeechRecognizer.ERROR_NO_MATCH -> "ERROR_NO_MATCH"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "ERROR_RECOGNIZER_BUSY"
                    SpeechRecognizer.ERROR_SERVER -> "ERROR_SERVER"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "ERROR_SPEECH_TIMEOUT"
                    else -> "UNKNOWN_ERROR($error)"
                }
                android.util.Log.e("SpeechRecognition", "onError: $errorMessage ($error)")
                isListening = false
                onResult(null)
            }
            
            override fun onResults(results: Bundle?) {
                android.util.Log.d("SpeechRecognition", "onResults")
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull()
                android.util.Log.d("SpeechRecognition", "Recognition result: $text")
                onResult(text)
            }
            
            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull()
                if (text != null) {
                    android.util.Log.d("SpeechRecognition", "Partial result: $text")
                    onPartialResult(text)
                }
            }
            
            override fun onEvent(eventType: Int, params: Bundle?) {
                android.util.Log.d("SpeechRecognition", "onEvent: $eventType")
            }
        })
        
        val startResult = speechRecognizer?.startListening(intent)
        android.util.Log.d("SpeechRecognition", "startListening called, result: $startResult")
    }
    
    fun stopListening() {
        speechRecognizer?.stopListening()
        isListening = false
    }
    
    fun cancel() {
        speechRecognizer?.cancel()
        isListening = false
    }
    
    fun release() {
        speechRecognizer?.cancel()
        speechRecognizer?.destroy()
        speechRecognizer = null
        isListening = false
    }
}

