package com.memorize.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface DeepSeekApi {
    @POST("chat/completions")
    suspend fun chatCompletions(
        @Header("Authorization") authorization: String,
        @Header("Content-Type") contentType: String = "application/json",
        @Body request: DeepSeekRequest
    ): Response<DeepSeekResponse>
}

data class DeepSeekRequest(
    val model: String = "deepseek-chat",
    val messages: List<DeepSeekMessage>,
    val temperature: Double = 0.6,
    val max_tokens: Int = 2000,
    val stream: Boolean = false
)

data class DeepSeekMessage(
    val role: String,
    val content: String
)

data class DeepSeekResponse(
    val id: String?,
    @com.google.gson.annotations.SerializedName("object") val objectType: String?,
    val created: Long?,
    val model: String?,
    val choices: List<DeepSeekChoice>?,
    val usage: DeepSeekUsage?
)

data class DeepSeekChoice(
    val index: Int?,
    val message: DeepSeekMessage?,
    val finish_reason: String?
)

data class DeepSeekUsage(
    val prompt_tokens: Int?,
    val completion_tokens: Int?,
    val total_tokens: Int?
)

