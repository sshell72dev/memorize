package com.memorize.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memorize.database.MemorizeDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PhraseItem(
    val order: Int,
    val text: String
)

data class ParagraphItem(
    val order: Int,
    val phrases: List<PhraseItem>
)

data class SectionItem(
    val order: Int,
    val paragraphs: List<ParagraphItem>
)

data class TextStructureUiState(
    val title: String = "",
    val isLoading: Boolean = true,
    val error: String? = null,
    val sections: List<SectionItem> = emptyList()
)

class TextStructureViewModel(
    private val database: MemorizeDatabase,
    private val textId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(TextStructureUiState())
    val uiState: StateFlow<TextStructureUiState> = _uiState.asStateFlow()

    init {
        loadStructure()
    }

    private fun loadStructure() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                val text = database.textDao().getTextById(textId)
                    ?: run {
                        _uiState.value = TextStructureUiState(
                            isLoading = false,
                            error = "Текст не найден"
                        )
                        return@launch
                    }

                val sections = database.sectionDao().getSectionsByTextIdOnce(textId)
                val sectionItems = sections.map { section ->
                    val paragraphs = database.paragraphDao().getParagraphsBySectionIdOnce(section.id)
                    val paragraphItems = paragraphs.map { paragraph ->
                        val phrases = database.phraseDao().getPhrasesByParagraphIdOnce(paragraph.id)
                        ParagraphItem(
                            order = paragraph.order,
                            phrases = phrases.map { phrase ->
                                PhraseItem(order = phrase.order, text = phrase.text)
                            }
                        )
                    }.sortedBy { it.order }

                    SectionItem(
                        order = section.order,
                        paragraphs = paragraphItems
                    )
                }.sortedBy { it.order }

                _uiState.value = TextStructureUiState(
                    title = text.title,
                    isLoading = false,
                    sections = sectionItems
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Ошибка загрузки структуры: ${e.message}"
                )
            }
        }
    }
}


