package com.memorize.network

data class ParsedTextStructure(
    val sections: List<SectionStructure>
)

data class SectionStructure(
    val paragraphs: List<ParagraphStructure>
)

data class ParagraphStructure(
    val phrases: List<String>
)

