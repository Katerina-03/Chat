package com.kate.chat.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

class ApiException(code: Int) : IOException("HTTP $code")

class ChatRepository(private val api: ChatApi) {

    @Volatile
    private var currentChats: List<Chat>? = null

    fun currentChats(): List<Chat>? = currentChats

    suspend fun chats(forceRefresh: Boolean = false): List<Chat> {
        if (!forceRefresh) currentChats?.let { return it }

        val loaded = coroutineScope {
            try {
                api.channels()
                    .map { name -> async { Chat(name, lastMessageOrNull(name)) } }
                    .awaitAll()
            } catch (e: HttpException) {
                throw ApiException(e.code())
            }
        }
        currentChats = loaded
        return loaded
    }

    suspend fun messages(chat: String, limit: Int, beforeId: Long?): List<Message> =
        withContext(Dispatchers.IO) {
            try {
                api.messages(chat = chat, limit = limit, lastKnownId = beforeId)
                    .map { dto ->
                        Message(
                            id = dto.id,
                            from = dto.from,
                            text = dto.data.textObj?.text,
                            imageLink = dto.data.imageObj?.link
                        )
                    }
            } catch (e: HttpException) {
                throw ApiException(e.code())
            }
        }

    suspend fun getThumbnail(path: String): Bitmap = loadBitmap {
        api.getThumbnail(path)
    }

    suspend fun getImage(path: String): Bitmap = loadBitmap {
        api.getImage(path)
    }

    private suspend fun loadBitmap(request: suspend () -> okhttp3.ResponseBody): Bitmap =
        withContext(Dispatchers.IO) {
            try {
                request().use { body ->
                    BitmapFactory.decodeStream(body.byteStream())
                        ?: throw IOException("Failed to decode bitmap")
                }
            } catch (e: HttpException) {
                throw ApiException(e.code())
            }
        }


    private suspend fun lastMessageOrNull(chat: String): Message? = try {
        messages(chat, 1, null).lastOrNull()
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }
}
