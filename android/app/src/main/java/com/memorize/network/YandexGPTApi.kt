package com.memorize.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface YandexGPTApi {
    @POST("foundationModels/v1/completion")
    suspend fun complete(
        @Header("Authorization") authorization: String,
        @Header("x-folder-id") folderId: String,
        @Body request: YandexGPTRequest
    ): Response<YandexGPTResponse>
}

data class YandexGPTRequest(
    val modelUri: String,
    val completionOptions: CompletionOptions,
    val messages: List<Message>
)

data class CompletionOptions(
    val stream: Boolean = false,
    val temperature: Double = 0.6,
    val maxTokens: String = "2000"
)

data class Message(
    val role: String,
    val text: String
)

data class YandexGPTResponse(
    val result: Result
)

data class Result(
    val alternatives: List<Alternative>
)

data class Alternative(
    val message: Message,
    val status: String
)

