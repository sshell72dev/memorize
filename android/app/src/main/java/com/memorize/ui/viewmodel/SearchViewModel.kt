package com.memorize.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memorize.database.MemorizeDatabase
import com.memorize.database.entity.TextEntity
import com.memorize.network.TextService
import com.memorize.network.YandexGPTService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SearchUiState(
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val savedTexts: List<TextEntity> = emptyList()
)

class SearchViewModel(
    private val database: MemorizeDatabase,
    private val textService: TextService
) : ViewModel() {
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()
    
    init {
        loadSavedTexts()
    }
    
    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query, error = null)
    }
    
    fun search() {
        val query = _uiState.value.searchQuery.trim()
        if (query.isBlank()) return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val textId = textService.loadAndSaveText(query)
                if (textId != null) {
                    // Navigate to learning screen will be handled by parent
                    _uiState.value = _uiState.value.copy(isLoading = false)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Не удалось найти текст. Попробуйте другое название."
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
    
    private fun loadSavedTexts() {
        viewModelScope.launch {
            database.textDao().getAllTexts().collect { texts ->
                _uiState.value = _uiState.value.copy(savedTexts = texts)
            }
        }
    }
}

