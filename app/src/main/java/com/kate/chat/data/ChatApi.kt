package com.kate.chat.data

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

interface ChatApi {

    @GET("channels")
    suspend fun channels(): List<String>

    @GET("channel/{chat}")
    suspend fun messages(
        @Path("chat") chat: String,
        @Query("limit") limit: Int,
        @Query("reverse") reverse: String = "true",
        @Query("lastKnownId") lastKnownId: Long?
    ): List<MessageResponseDto>

    @Streaming
    @GET("thumb/{path}")
    suspend fun getThumbnail(
        @Path("path", encoded = true) path: String
    ): ResponseBody

    @Streaming
    @GET("img/{path}")
    suspend fun getImage(
        @Path("path", encoded = true) path: String
    ): ResponseBody
}
