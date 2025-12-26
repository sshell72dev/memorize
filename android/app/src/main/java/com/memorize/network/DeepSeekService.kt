package com.memorize.network

import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class DeepSeekService(
    private val apiKey: String
) {
    private val baseUrl = "https://api.deepseek.com/v1/"
    
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
    
    private val api: DeepSeekApi = retrofit.create(DeepSeekApi::class.java)
    
    suspend fun getTextByTitle(title: String, author: String? = null): String? {
        // Build a clear prompt for finding the text
        var prompt = "Найди и верни полный текст произведения или стихотворения"
        
        if (!author.isNullOrBlank()) {
            prompt += " автора $author"
        }
        
        prompt += " с названием: $title"
        prompt += ". Верни только текст без дополнительных комментариев, без названия и автора в начале."
        
        val request = DeepSeekRequest(
            model = "deepseek-chat",
            messages = listOf(
                DeepSeekMessage(role = "user", content = prompt)
            ),
            temperature = 0.6,
            max_tokens = 2000,
            stream = false
        )
        
        // Retry logic - sometimes API has temporary issues
        var lastException: Exception? = null
        repeat(2) { attempt ->
            try {
                android.util.Log.d("DeepSeek", "Sending request (attempt ${attempt + 1}/2) with prompt: $prompt")
                android.util.Log.d("DeepSeek", "API Key prefix: ${if (apiKey.length > 10) apiKey.take(10) + "..." else "INVALID"}")
                
                val response = api.chatCompletions(
                    authorization = "Bearer $apiKey",
                    request = request
                )
                
                android.util.Log.d("DeepSeek", "Response code: ${response.code()}, isSuccessful: ${response.isSuccessful}")
                
                if (response.isSuccessful) {
                    val text = response.body()?.choices?.firstOrNull()?.message?.content
                    android.util.Log.d("DeepSeek", "Received text length: ${text?.length ?: 0}")
                    return text
                } else {
                    val errorBody = response.errorBody()?.string()
                    android.util.Log.e("DeepSeek", "Request failed: ${response.code()} - ${response.message()}")
                    android.util.Log.e("DeepSeek", "Error body: $errorBody")
                    
                    // Check for specific error codes and provide helpful messages
                    when (response.code()) {
                        401 -> {
                            android.util.Log.e("DeepSeek", "401 Unauthorized - Invalid or expired API key")
                            android.util.Log.e("DeepSeek", "Check that API key in config.xml is correct")
                        }
                        400 -> {
                            android.util.Log.e("DeepSeek", "400 Bad Request - Check request format")
                        }
                        429 -> {
                            android.util.Log.e("DeepSeek", "429 Too Many Requests - Rate limit exceeded, will retry")
                            if (attempt < 1) {
                                kotlinx.coroutines.delay(1000) // Wait 1 second before retry
                                return@repeat
                            }
                        }
                        500, 502, 503 -> {
                            android.util.Log.e("DeepSeek", "${response.code()} Server Error - Temporary issue, will retry")
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
                android.util.Log.e("DeepSeek", "Exception in getTextByTitle (attempt ${attempt + 1})", e)
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
        
        val request = DeepSeekRequest(
            model = "deepseek-chat",
            messages = listOf(
                DeepSeekMessage(role = "user", content = prompt)
            ),
            temperature = 0.6,
            max_tokens = 2000,
            stream = false
        )
        
        return try {
            android.util.Log.d("DeepSeek", "parseText: Sending request to parse text (length: ${text.length})")
            val response = api.chatCompletions(
                authorization = "Bearer $apiKey",
                request = request
            )
            
            android.util.Log.d("DeepSeek", "parseText: Response code: ${response.code()}, isSuccessful: ${response.isSuccessful}")
            
            if (response.isSuccessful) {
                val jsonResponse = response.body()?.choices?.firstOrNull()?.message?.content
                android.util.Log.d("DeepSeek", "parseText: Received JSON response length: ${jsonResponse?.length ?: 0}")
                if (jsonResponse != null) {
                    val parsed = TextParser.parseJson(jsonResponse)
                    android.util.Log.d("DeepSeek", "parseText: Parsed successfully: ${parsed != null}")
                    parsed
                } else {
                    android.util.Log.e("DeepSeek", "parseText: JSON response is null")
                    null
                }
            } else {
                val errorBody = response.errorBody()?.string()
                android.util.Log.e("DeepSeek", "parseText: Request failed: ${response.code()} - ${response.message()}")
                android.util.Log.e("DeepSeek", "parseText: Error body: $errorBody")
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("DeepSeek", "parseText: Exception", e)
            e.printStackTrace()
            null
        }
    }
}

