package com.memorize.learning

data class LearningState(
    val currentPhraseText: String = "",
    val isListening: Boolean = false,
    val feedback: String? = null,
    val isCorrect: Boolean = false,
    val canContinue: Boolean = false,
    val currentSection: Int = 0,
    val totalSections: Int = 0,
    val currentParagraph: Int = 0,
    val totalParagraphs: Int = 0,
    val currentPhrase: Int = 0,
    val totalPhrases: Int = 0
)

