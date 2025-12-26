package com.memorize.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memorize.database.MemorizeDatabase
import com.memorize.database.entity.TextEntity
import com.memorize.network.TextService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

data class FoundText(
    val title: String,
    val author: String?,
    val fullText: String
)

data class SearchUiState(
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val savedTexts: List<TextEntity> = emptyList(),
    val foundText: FoundText? = null
)

class SearchViewModel(
    private val database: MemorizeDatabase,
    private val textService: TextService,
    private val context: android.content.Context
) : ViewModel() {
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()
    private var savedTextsJob: Job? = null
    
    init {
        Log.d("Memorize", "SearchViewModel.init started")
        try {
            loadSavedTexts()
        } catch (e: Exception) {
            Log.e("Memorize", "Error in SearchViewModel.init", e)
        }
    }
    
    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query, error = null, foundText = null)
    }
    
    fun clearFoundText() {
        _uiState.value = _uiState.value.copy(foundText = null)
    }
    
    fun deleteText(textId: String) {
        viewModelScope.launch {
            try {
                android.util.Log.d("SearchViewModel", "deleteText: Deleting text with id: $textId")
                val userId = getUserId()
                val text = database.textDao().getTextById(textId)
                if (text != null && text.userId == userId) {
                    database.textDao().deleteText(text)
                    android.util.Log.d("SearchViewModel", "deleteText: Text deleted successfully")
                    // Refresh the list
                    refreshSavedTexts()
                } else {
                    android.util.Log.e("SearchViewModel", "deleteText: Text not found or belongs to different user")
                }
            } catch (e: Exception) {
                android.util.Log.e("SearchViewModel", "deleteText: Error deleting text", e)
                e.printStackTrace()
            }
        }
    }
    
    fun refreshSavedTexts() {
        // Cancel existing job if any
        savedTextsJob?.cancel()
        // Small delay to ensure cancellation is processed
        // Then reload texts
        viewModelScope.launch {
            try {
                kotlinx.coroutines.delay(50) // Small delay to ensure previous job is cancelled
                loadSavedTexts()
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Ignore cancellation
                Log.d("Memorize", "refreshSavedTexts: Cancelled")
            } catch (e: Exception) {
                Log.e("Memorize", "Error in refreshSavedTexts", e)
                e.printStackTrace()
            }
        }
    }
    
    fun search() {
        val query = _uiState.value.searchQuery.trim()
        if (query.isBlank()) return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, foundText = null)
            
            try {
                android.util.Log.d("SearchViewModel", "Searching for: $query")
                
                // Parse query to extract title and author
                val (title, author) = parseQuery(query)
                android.util.Log.d("SearchViewModel", "Parsed - title: '$title', author: '$author'")
                
                // Get text from AI and save immediately
                android.util.Log.d("SearchViewModel", "Calling textService.getTextByTitle")
                val fullText = textService.getTextByTitle(title, author)
                android.util.Log.d("SearchViewModel", "getTextByTitle returned: ${if (fullText != null) "text length ${fullText.length}" else "null"}")
                
                if (fullText != null && fullText.isNotBlank()) {
                    android.util.Log.d("SearchViewModel", "Text found successfully, saving immediately")
                    // Save text immediately after receiving from AI
                    val textId = textService.saveTextFromFullText(title, fullText)
                    android.util.Log.d("SearchViewModel", "Text saved with id: $textId")
                    
                    // Refresh saved texts list to show the new text
                    refreshSavedTexts()
                    
                    // Clear search query and show success
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        searchQuery = "", // Clear search field
                        error = null,
                        foundText = null // No need for preview anymore
                    )
                } else {
                    android.util.Log.w("SearchViewModel", "Text not found or empty")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Не удалось найти текст. Попробуйте другое название или укажите автора."
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("SearchViewModel", "Error in search", e)
                val errorMessage = when {
                    e is retrofit2.HttpException && e.code() == 401 -> {
                        "Неверный API ключ (401). Проверьте, что ключ в config.xml правильный"
                    }
                    e is retrofit2.HttpException && e.code() == 400 -> {
                        "Неверный запрос (400). Попробуйте другой поисковый запрос."
                    }
                    e is retrofit2.HttpException && e.code() == 429 -> {
                        "Превышен лимит запросов (429). Подождите немного и попробуйте снова."
                    }
                    else -> {
                        "Ошибка: ${e.message ?: e.javaClass.simpleName}"
                    }
                }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = errorMessage
                )
            }
        }
    }
    
    suspend fun approveAndSave(foundText: FoundText? = null): String? {
        android.util.Log.d("SearchViewModel", "approveAndSave: called with foundText: ${foundText?.title ?: "null"}")
        val found = foundText ?: _uiState.value.foundText
        android.util.Log.d("SearchViewModel", "approveAndSave: Using found: ${found?.title ?: "null"}")
        if (found == null) {
            android.util.Log.e("SearchViewModel", "approveAndSave: foundText is null")
            return null
        }
        
        android.util.Log.d("SearchViewModel", "approveAndSave: Saving text - title: '${found.title}', text length: ${found.fullText.length}")
        
        try {
            // Use already fetched text instead of requesting again
            android.util.Log.d("SearchViewModel", "approveAndSave: Calling textService.saveTextFromFullText")
            val textId = textService.saveTextFromFullText(found.title, found.fullText)
            android.util.Log.d("SearchViewModel", "approveAndSave: saveTextFromFullText returned: $textId")
            if (textId == null) {
                android.util.Log.e("SearchViewModel", "approveAndSave: saveTextFromFullText returned null")
            } else {
                android.util.Log.d("SearchViewModel", "approveAndSave: Text saved successfully with id: $textId")
            }
            return textId
        } catch (e: Exception) {
            android.util.Log.e("SearchViewModel", "approveAndSave: Error saving text", e)
            e.printStackTrace()
            return null
        }
    }
    
    private fun parseQuery(query: String): Pair<String, String?> {
        val trimmedQuery = query.trim()
        
        // Check if query contains author indicators like "автор:", "by", etc.
        val authorKeyword = "автор:"
        val authorIndex = trimmedQuery.indexOf(authorKeyword, ignoreCase = true)
        if (authorIndex >= 0) {
            val afterKeyword = trimmedQuery.substring(authorIndex + authorKeyword.length).trim()
            val beforeKeyword = trimmedQuery.substring(0, authorIndex).trim()
            
            if (afterKeyword.isNotEmpty()) {
                if (beforeKeyword.isEmpty()) {
                    // "автор: Пушкин" - everything after is author, but we need title
                    // Try to split: last word might be title
                    val parts = afterKeyword.split("\\s+".toRegex())
                    if (parts.size > 1) {
                        val author = parts.dropLast(1).joinToString(" ")
                        val title = parts.last()
                        return Pair(title, author)
                    }
                    return Pair(afterKeyword, null) // Can't determine, use as title
                } else {
                    // "Зимнее утро автор: Пушкин" or "фраза из текста автор: Пушкин"
                    return Pair(beforeKeyword, afterKeyword)
                }
            }
        }
        
        // Check for "by" keyword (English)
        val byIndex = trimmedQuery.indexOf(" by ", ignoreCase = true)
        if (byIndex >= 0) {
            val beforeBy = trimmedQuery.substring(0, byIndex).trim()
            val afterBy = trimmedQuery.substring(byIndex + 4).trim()
            if (beforeBy.isNotEmpty() && afterBy.isNotEmpty()) {
                return Pair(beforeBy, afterBy)
            }
        }
        
        val parts = trimmedQuery.split("\\s+".toRegex()).filter { it.isNotEmpty() }
        
        // If query is short (1-2 words), assume it's just title/phrase
        if (parts.size <= 2) {
            return Pair(trimmedQuery, null)
        }
        
        // For longer queries, try heuristic: last 1-2 words might be author
        // Common pattern: "название произведения автор фамилия" or "фраза из текста автор фамилия"
        if (parts.size >= 3) {
            // Try last word as author
            val possibleAuthor = parts.last()
            val possibleTitle = parts.dropLast(1).joinToString(" ")
            
            // If last word looks like a name (capitalized, short), it might be author
            if (possibleAuthor.length <= 15 && possibleAuthor.firstOrNull()?.isUpperCase() == true) {
                return Pair(possibleTitle, possibleAuthor)
            }
            
            // Try last 2 words as author (e.g., "А.С. Пушкин")
            if (parts.size >= 4) {
                val possibleAuthor2 = parts.takeLast(2).joinToString(" ")
                val possibleTitle2 = parts.dropLast(2).joinToString(" ")
                // Check if last 2 words look like author name (at least one capitalized)
                if (possibleAuthor2.split(" ").any { it.firstOrNull()?.isUpperCase() == true }) {
                    return Pair(possibleTitle2, possibleAuthor2)
                }
            }
        }
        
        // Default: use entire query as title/phrase (could be a phrase from the work)
        // The DeepSeekService will handle searching by phrase
        return Pair(trimmedQuery, null)
    }
    
    private fun loadSavedTexts() {
        // Cancel existing job if any
        savedTextsJob?.cancel()
        // Start new collection job
        savedTextsJob = viewModelScope.launch {
            try {
                Log.d("Memorize", "loadSavedTexts: Starting to collect texts")
                // Get userId from TextService - we need to pass it
                // For now, use a default or get from shared preferences
                val userId = getUserId()
                Log.d("Memorize", "loadSavedTexts: Using userId: $userId")
                database.textDao().getAllTexts(userId).collect { texts ->
                    try {
                        Log.d("Memorize", "loadSavedTexts: Received ${texts.size} texts")
                        _uiState.value = _uiState.value.copy(savedTexts = texts)
                    } catch (e: Exception) {
                        // Ignore cancellation exceptions - they are normal when job is cancelled
                        if (e !is kotlinx.coroutines.CancellationException) {
                            Log.e("Memorize", "Error updating state in loadSavedTexts", e)
                            e.printStackTrace()
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore cancellation exceptions - they are normal when job is cancelled
                if (e !is kotlinx.coroutines.CancellationException) {
                    Log.e("Memorize", "Error in loadSavedTexts", e)
                    e.printStackTrace()
                    try {
                        _uiState.value = _uiState.value.copy(
                            error = "Ошибка загрузки текстов: ${e.message ?: "Неизвестная ошибка"}"
                        )
                    } catch (stateError: Exception) {
                        if (stateError !is kotlinx.coroutines.CancellationException) {
                            Log.e("Memorize", "Error updating error state", stateError)
                        }
                    }
                } else {
                    // Cancellation is normal, just log at debug level
                    Log.d("Memorize", "loadSavedTexts: Job was cancelled (this is normal)")
                }
            }
        }
    }
    
    private fun getUserId(): String {
        // Get userId from shared preferences or generate one
        val prefs = context.getSharedPreferences("memorize_prefs", android.content.Context.MODE_PRIVATE)
        var userId = prefs.getString("user_id", null)
        if (userId == null) {
            userId = java.util.UUID.randomUUID().toString()
            prefs.edit().putString("user_id", userId).apply()
        }
        return userId
    }
}

