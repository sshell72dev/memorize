package com.memorize.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class YandexGPTService(
    private val apiKey: String,
    private val folderId: String
) {
    private val baseUrl = "https://llm.api.cloud.yandex.net/"
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    private val api: YandexGPTApi = retrofit.create(YandexGPTApi::class.java)
    
    suspend fun getTextByTitle(title: String): String? {
        val prompt = "Найди и верни полный текст произведения или стихотворения с названием: $title. Верни только текст без дополнительных комментариев."
        
        val request = YandexGPTRequest(
            modelUri = "gpt://$folderId/yandexgpt/latest",
            completionOptions = CompletionOptions(),
            messages = listOf(
                Message(role = "user", text = prompt)
            )
        )
        
        return try {
            val response = api.complete(
                authorization = "Api-Key $apiKey",
                folderId = folderId,
                request = request
            )
            
            if (response.isSuccessful) {
                response.body()?.result?.alternatives?.firstOrNull()?.message?.text
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    suspend fun parseText(text: String): ParsedTextStructure? {
        val prompt = """
            Разбей следующий текст на:
            1. Разделы (логические части)
            2. Абзацы/четверостишия внутри разделов
            3. Фразы/предложения внутри абзацев
            
            Верни в формате JSON:
            {
              "sections": [
                {
                  "paragraphs": [
                    {
                      "phrases": ["фраза 1", "фраза 2"]
                    }
                  ]
                }
              ]
            }
            
            Текст:
            $text
        """.trimIndent()
        
        val request = YandexGPTRequest(
            modelUri = "gpt://$folderId/yandexgpt/latest",
            completionOptions = CompletionOptions(),
            messages = listOf(
                Message(role = "user", text = prompt)
            )
        )
        
        return try {
            val response = api.complete(
                authorization = "Api-Key $apiKey",
                folderId = folderId,
                request = request
            )
            
            if (response.isSuccessful) {
                val jsonResponse = response.body()?.result?.alternatives?.firstOrNull()?.message?.text
                jsonResponse?.let { TextParser.parseJson(it) }
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

data class ParsedTextStructure(
    val sections: List<SectionStructure>
)

data class SectionStructure(
    val paragraphs: List<ParagraphStructure>
)

data class ParagraphStructure(
    val phrases: List<String>
)
