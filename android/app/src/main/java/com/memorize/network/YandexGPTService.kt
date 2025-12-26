package com.memorize.network

import kotlinx.coroutines.delay
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
    
    suspend fun getTextByTitle(title: String, author: String? = null): String? {
        // Build a clear prompt for finding the text - simplified version that worked before
        var prompt = "Найди и верни полный текст произведения или стихотворения"
        
        if (!author.isNullOrBlank()) {
            prompt += " автора $author"
        }
        
        prompt += " с названием: $title"
        prompt += ". Верни только текст без дополнительных комментариев, без названия и автора в начале."
        
        val request = YandexGPTRequest(
            modelUri = "gpt://$folderId/yandexgpt/latest",
            completionOptions = CompletionOptions(),
            messages = listOf(
                Message(role = "user", text = prompt)
            )
        )
        
        // Retry logic - sometimes API has temporary issues
        var lastException: Exception? = null
        repeat(2) { attempt ->
            try {
                android.util.Log.d("YandexGPT", "Sending request (attempt ${attempt + 1}/2) with prompt: $prompt")
                android.util.Log.d("YandexGPT", "Using folder ID: $folderId")
                android.util.Log.d("YandexGPT", "API Key prefix: ${if (apiKey.length > 10) apiKey.take(10) + "..." else "INVALID"}")
                
                val response = api.complete(
                    authorization = "Api-Key $apiKey",
                    folderId = folderId,
                    request = request
                )
                
                android.util.Log.d("YandexGPT", "Response code: ${response.code()}, isSuccessful: ${response.isSuccessful}")
                
                if (response.isSuccessful) {
                    val text = response.body()?.result?.alternatives?.firstOrNull()?.message?.text
                    android.util.Log.d("YandexGPT", "Received text length: ${text?.length ?: 0}")
                    return text
                } else {
                    val errorBody = response.errorBody()?.string()
                    android.util.Log.e("YandexGPT", "Request failed: ${response.code()} - ${response.message()}")
                    android.util.Log.e("YandexGPT", "Error body: $errorBody")
                    
                    // Check for specific error codes and provide helpful messages
                    when (response.code()) {
                        403 -> {
                            android.util.Log.e("YandexGPT", "403 Forbidden - API key does not have access to folder $folderId")
                            android.util.Log.e("YandexGPT", "This worked yesterday - check if API key or permissions changed")
                        }
                        401 -> {
                            android.util.Log.e("YandexGPT", "401 Unauthorized - Invalid or expired API key")
                            android.util.Log.e("YandexGPT", "Check that API key in config.xml matches the key in Yandex Cloud Console")
                        }
                        400 -> {
                            android.util.Log.e("YandexGPT", "400 Bad Request - Check request format")
                            android.util.Log.e("YandexGPT", "Request: modelUri=gpt://$folderId/yandexgpt/latest")
                        }
                        429 -> {
                            android.util.Log.e("YandexGPT", "429 Too Many Requests - Rate limit exceeded, will retry")
                            if (attempt < 1) {
                                kotlinx.coroutines.delay(1000) // Wait 1 second before retry
                                return@repeat
                            }
                        }
                        500, 502, 503 -> {
                            android.util.Log.e("YandexGPT", "${response.code()} Server Error - Temporary issue, will retry")
                            if (attempt < 1) {
                                kotlinx.coroutines.delay(1000) // Wait 1 second before retry
                                return@repeat
                            }
                        }
                    }
                    // For non-retryable errors, return null immediately
                    if (response.code() !in listOf(429, 500, 502, 503)) {
                        return null
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("YandexGPT", "Exception in getTextByTitle (attempt ${attempt + 1})", e)
                lastException = e
                if (attempt < 1) {
                    kotlinx.coroutines.delay(1000) // Wait 1 second before retry
                }
            }
        }
        
        // If we get here, all attempts failed
        lastException?.printStackTrace()
        return null
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
