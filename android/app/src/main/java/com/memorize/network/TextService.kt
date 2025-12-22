package com.memorize.network

import com.memorize.database.MemorizeDatabase
import com.memorize.database.entity.*
import java.util.UUID

class TextService(
    private val database: MemorizeDatabase,
    private val yandexGPTService: YandexGPTService
) {
    suspend fun loadAndSaveText(title: String): String? {
        // Check if text already exists
        val existingText = database.textDao().getTextById(title)
        if (existingText != null) {
            return existingText.id
        }
        
        // Get text from Yandex GPT
        val fullText = yandexGPTService.getTextByTitle(title) ?: return null
        
        // Parse text
        val parsedStructure = yandexGPTService.parseText(fullText) ?: return null
        
        // Save to database
        val textId = UUID.randomUUID().toString()
        val textEntity = TextEntity(
            id = textId,
            title = title,
            fullText = fullText
        )
        database.textDao().insertText(textEntity)
        
        // Save sections, paragraphs, and phrases
        parsedStructure.sections.forEachIndexed { sectionIndex, section ->
            val sectionId = UUID.randomUUID().toString()
            val sectionEntity = SectionEntity(
                id = sectionId,
                textId = textId,
                order = sectionIndex
            )
            database.sectionDao().insertSection(sectionEntity)
            
            section.paragraphs.forEachIndexed { paragraphIndex, paragraph ->
                val paragraphId = UUID.randomUUID().toString()
                val paragraphEntity = ParagraphEntity(
                    id = paragraphId,
                    sectionId = sectionId,
                    order = paragraphIndex
                )
                database.paragraphDao().insertParagraph(paragraphEntity)
                
                paragraph.phrases.forEachIndexed { phraseIndex, phraseText ->
                    val phraseId = UUID.randomUUID().toString()
                    val phraseEntity = PhraseEntity(
                        id = phraseId,
                        paragraphId = paragraphId,
                        order = phraseIndex,
                        text = phraseText
                    )
                    database.phraseDao().insertPhrase(phraseEntity)
                }
            }
        }
        
        return textId
    }
    
    suspend fun saveTextDirectly(title: String, fullText: String): String? {
        // Check if text already exists
        val existingText = database.textDao().getTextById(title)
        if (existingText != null) {
            return existingText.id
        }
        
        // Parse text using AI
        val parsedStructure = yandexGPTService.parseText(fullText) ?: return null
        
        // Save to database
        val textId = UUID.randomUUID().toString()
        val textEntity = TextEntity(
            id = textId,
            title = title,
            fullText = fullText
        )
        database.textDao().insertText(textEntity)
        
        // Save sections, paragraphs, and phrases
        parsedStructure.sections.forEachIndexed { sectionIndex, section ->
            val sectionId = UUID.randomUUID().toString()
            val sectionEntity = SectionEntity(
                id = sectionId,
                textId = textId,
                order = sectionIndex
            )
            database.sectionDao().insertSection(sectionEntity)
            
            section.paragraphs.forEachIndexed { paragraphIndex, paragraph ->
                val paragraphId = UUID.randomUUID().toString()
                val paragraphEntity = ParagraphEntity(
                    id = paragraphId,
                    sectionId = sectionId,
                    order = paragraphIndex
                )
                database.paragraphDao().insertParagraph(paragraphEntity)
                
                paragraph.phrases.forEachIndexed { phraseIndex, phraseText ->
                    val phraseId = UUID.randomUUID().toString()
                    val phraseEntity = PhraseEntity(
                        id = phraseId,
                        paragraphId = paragraphId,
                        order = phraseIndex,
                        text = phraseText
                    )
                    database.phraseDao().insertPhrase(phraseEntity)
                }
            }
        }
        
        return textId
    }
}

