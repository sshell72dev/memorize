package com.memorize.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.memorize.database.MemorizeDatabase
import com.memorize.learning.LearningFlowController
import com.memorize.learning.LearningState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LearningUiState(
    val currentPhraseText: String = "",
    val displayedText: String = "",
    val userSpokenText: String = "",
    val isSpeaking: Boolean = false,
    val isListening: Boolean = false,
    val feedback: String? = null,
    val isCorrect: Boolean = false,
    val canContinue: Boolean = false,
    val currentSection: Int = 0,
    val totalSections: Int = 0,
    val currentParagraph: Int = 0,
    val totalParagraphs: Int = 0,
    val currentPhrase: Int = 0,
    val totalPhrases: Int = 0,
    val audioLevel: Float = 0f,
    val currentPhase: com.memorize.learning.LearningPhase = com.memorize.learning.LearningPhase.PASS1,
    val showPass2Instruction: Boolean = false,
    val pass2InstructionText: String = ""
)

class LearningViewModel(
    application: Application,
    private val database: MemorizeDatabase,
    private val textId: String,
    private val onComplete: (String) -> Unit
) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(LearningUiState())
    val uiState: StateFlow<LearningUiState> = _uiState.asStateFlow()
    
    private val learningController = LearningFlowController(
        context = application.applicationContext,
        database = database,
        textId = textId,
        onComplete = onComplete
    )
    
    init {
        viewModelScope.launch {
            learningController.initialize { state ->
                _uiState.value = convertState(state)
            }
        }
    }
    
    fun onDontRemember() {
        viewModelScope.launch {
            learningController.onDontRemember { state ->
                _uiState.value = convertState(state)
            }
        }
    }
    
    fun continueToNext() {
        viewModelScope.launch {
            learningController.continueToNext { state ->
                _uiState.value = convertState(state)
            }
        }
    }
    
    fun startPass2AfterInstruction() {
        viewModelScope.launch {
            learningController.startPass2AfterInstruction { state ->
                _uiState.value = convertState(state)
            }
        }
    }
    
    private fun convertState(state: LearningState): LearningUiState {
        return LearningUiState(
            currentPhraseText = state.currentPhraseText,
            displayedText = state.displayedText,
            userSpokenText = state.userSpokenText,
            isSpeaking = state.isSpeaking,
            isListening = state.isListening,
            feedback = state.feedback,
            isCorrect = state.isCorrect,
            canContinue = state.canContinue,
            currentSection = state.currentSection,
            totalSections = state.totalSections,
            currentParagraph = state.currentParagraph,
            totalParagraphs = state.totalParagraphs,
            currentPhrase = state.currentPhrase,
            totalPhrases = state.totalPhrases,
            audioLevel = state.audioLevel,
            currentPhase = state.currentPhase,
            showPass2Instruction = state.showPass2Instruction,
            pass2InstructionText = state.pass2InstructionText
        )
    }
}

