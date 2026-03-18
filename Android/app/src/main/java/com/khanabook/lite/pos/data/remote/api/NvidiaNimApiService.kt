package com.khanabook.lite.pos.data.remote.api

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface NvidiaNimApiService {
    @POST("chat/completions")
    suspend fun getChatCompletions(
        @Header("Authorization") authorization: String,
        @Body request: NvidiaChatRequest
    ): NvidiaChatResponse
}

data class NvidiaChatRequest(
    val model: String = "meta/llama-3.1-8b-instruct",
    val messages: List<NvidiaMessage>,
    val temperature: Double = 0.2,
    val top_p: Double = 0.7,
    val max_tokens: Int = 1024,
    val stream: Boolean = false
)

data class NvidiaMessage(
    val role: String,
    val content: String
)

data class NvidiaChatResponse(
    val id: String?,
    val choices: List<NvidiaChoice>?,
    val usage: NvidiaUsage?
)

data class NvidiaChoice(
    val index: Int?,
    val message: NvidiaMessage?,
    val finish_reason: String?
)

data class NvidiaUsage(
    val prompt_tokens: Int?,
    val completion_tokens: Int?,
    val total_tokens: Int?
)
