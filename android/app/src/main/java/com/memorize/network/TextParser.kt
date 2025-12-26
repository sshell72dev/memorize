package com.memorize.network

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser

object TextParser {
    private val gson = Gson()
    
    fun parseJson(jsonString: String): ParsedTextStructure? {
        return try {
            // Try to extract JSON from markdown code blocks if present
            val cleanedJson = jsonString
                .replace("```json", "")
                .replace("```", "")
                .trim()
            
            val jsonObject = JsonParser().parse(cleanedJson).asJsonObject
            val sectionsArray = jsonObject.getAsJsonArray("sections")
            
            val sections = sectionsArray.map { sectionElement ->
                val sectionObj = sectionElement.asJsonObject
                val paragraphsArray = sectionObj.getAsJsonArray("paragraphs")
                
                val paragraphs = paragraphsArray.map { paragraphElement ->
                    val paragraphObj = paragraphElement.asJsonObject
                    val phrasesArray = paragraphObj.getAsJsonArray("phrases")
                    
                    val phrases = phrasesArray.map { phraseElement -> phraseElement.asString }
                    ParagraphStructure(phrases)
                }
                
                SectionStructure(paragraphs)
            }
            
            ParsedTextStructure(sections)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

