package com.memorize.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memorize.database.MemorizeDatabase
import com.memorize.network.TextService
import com.memorize.network.YandexGPTService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TextInputUiState(
    val title: String = "",
    val textContent: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

class TextInputViewModel(
    private val database: MemorizeDatabase,
    private val textService: TextService
) : ViewModel() {
    private val _uiState = MutableStateFlow(TextInputUiState())
    val uiState: StateFlow<TextInputUiState> = _uiState.asStateFlow()
    
    fun updateTitle(title: String) {
        _uiState.value = _uiState.value.copy(title = title, error = null)
    }
    
    fun updateTextContent(textContent: String) {
        _uiState.value = _uiState.value.copy(textContent = textContent, error = null)
    }
    
    fun saveText(onTextSaved: (String) -> Unit) {
        val title = _uiState.value.title.trim()
        val textContent = _uiState.value.textContent.trim()
        
        if (title.isBlank() || textContent.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Заполните все поля")
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Save text directly without searching via AI
                val textId = textService.saveTextDirectly(title, textContent)
                if (textId != null) {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    onTextSaved(textId)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Не удалось сохранить текст"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Ошибка: ${e.message}"
                )
            }
        }
    }
}

