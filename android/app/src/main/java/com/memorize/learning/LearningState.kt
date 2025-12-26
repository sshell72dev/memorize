package com.memorize.learning

data class LearningState(
    val currentPhraseText: String = "",
    val displayedText: String = "", // Text displayed with typing animation
    val userSpokenText: String = "", // Text user is speaking
    val isSpeaking: Boolean = false, // TTS is speaking
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
    val audioLevel: Float = 0f, // For visualization (0-1)
    val currentPhase: LearningPhase = LearningPhase.PASS1, // Current learning phase
    val showPass2Instruction: Boolean = false, // Show instruction screen before Pass2
    val pass2InstructionText: String = "" // Full text to be recited in Pass2
)

