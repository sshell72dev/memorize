package com.memorize.network

import com.memorize.database.MemorizeDatabase
import com.memorize.database.entity.*
import java.util.UUID

class TextService(
    private val database: MemorizeDatabase,
    private val deepSeekService: DeepSeekService,
    private val userId: String,
    private val context: android.content.Context? = null
) {
    suspend fun getTextByTitle(title: String, author: String? = null): String? {
        android.util.Log.d("TextService", "getTextByTitle called - title: '$title', author: '$author'")
        return try {
            // Get text from DeepSeek without saving
            val result = deepSeekService.getTextByTitle(title, author)
            android.util.Log.d("TextService", "getTextByTitle result: ${if (result != null) "text length ${result.length}" else "null"}")
            result
        } catch (e: Exception) {
            android.util.Log.e("TextService", "Error in getTextByTitle", e)
            throw e // Re-throw to be handled by SearchViewModel
        }
    }
    
    suspend fun loadAndSaveText(title: String, author: String? = null): String? {
        // Check if text already exists for this user
        val existingText = database.textDao().getTextByTitle(userId, title)
        if (existingText != null) {
            return existingText.id
        }
        
        // Get text from DeepSeek
        val fullText = deepSeekService.getTextByTitle(title, author) ?: return null
        
        // Parse and save
        return saveTextFromFullText(title, fullText)
    }
    
    suspend fun saveTextFromFullText(title: String, fullText: String): String? {
        android.util.Log.d("TextService", "saveTextFromFullText: title='$title', text length=${fullText.length}, userId=$userId")
        
        // Check if text already exists for this user
        val existingText = database.textDao().getTextByTitle(userId, title)
        if (existingText != null) {
            android.util.Log.d("TextService", "saveTextFromFullText: Text already exists, returning existing id: ${existingText.id}")
            return existingText.id
        }
        
        // Save text first
        val textId = UUID.randomUUID().toString()
        val textEntity = TextEntity(
            id = textId,
            userId = userId,
            title = title,
            fullText = fullText
        )
        database.textDao().insertText(textEntity)
        android.util.Log.d("TextService", "saveTextFromFullText: Text entity saved with id: $textId")
        
        // Create simple structure immediately (fast, no API call)
        android.util.Log.d("TextService", "saveTextFromFullText: Creating simple structure...")
        val sentences = fullText.split(Regex("[.!?]+")).filter { it.trim().isNotEmpty() }
        if (sentences.isNotEmpty()) {
            // Get phrases per paragraph from settings (default: 4)
            val phrasesPerParagraph = context?.let {
                it.getSharedPreferences("memorize_prefs", android.content.Context.MODE_PRIVATE)
                    .getInt("phrases_per_paragraph", 4)
            } ?: 4
            
            android.util.Log.d("TextService", "saveTextFromFullText: Using $phrasesPerParagraph phrases per paragraph")
            
            val sectionId = UUID.randomUUID().toString()
            val sectionEntity = SectionEntity(
                id = sectionId,
                textId = textId,
                order = 0
            )
            database.sectionDao().insertSection(sectionEntity)
            
            // Split sentences into paragraphs based on phrasesPerParagraph setting
            sentences.chunked(phrasesPerParagraph).forEachIndexed { paragraphIndex, paragraphSentences ->
                val paragraphId = UUID.randomUUID().toString()
                val paragraphEntity = ParagraphEntity(
                    id = paragraphId,
                    sectionId = sectionId,
                    order = paragraphIndex
                )
                database.paragraphDao().insertParagraph(paragraphEntity)
                
                paragraphSentences.forEachIndexed { phraseIndex, sentence ->
                    val phraseId = UUID.randomUUID().toString()
                    val phraseEntity = PhraseEntity(
                        id = phraseId,
                        paragraphId = paragraphId,
                        order = phraseIndex,
                        text = sentence.trim()
                    )
                    database.phraseDao().insertPhrase(phraseEntity)
                }
            }
            android.util.Log.d("TextService", "saveTextFromFullText: Created structure with ${sentences.size} phrases in ${(sentences.size + phrasesPerParagraph - 1) / phrasesPerParagraph} paragraphs")
        }
        
        android.util.Log.d("TextService", "saveTextFromFullText: Text saved successfully with id: $textId")
        return textId
    }
    
    suspend fun saveTextDirectly(title: String, fullText: String): String? {
        // Use the same method as loadAndSaveText
        return saveTextFromFullText(title, fullText)
    }
}

