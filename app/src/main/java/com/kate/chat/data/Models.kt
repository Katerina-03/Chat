package com.kate.chat.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class Message(
    val id: Long,
    val from: String,
    val text: String?,
    val imageLink: String?
)

data class Chat(
    val name: String,
    val lastMessage: Message?
)

@Serializable
data class TextWrapper(@SerialName("text") val text: String)

@Serializable
data class ImageWrapper(@SerialName("link") val link: String)

@Serializable
data class MessageDataDto(
    @SerialName("Text") val textObj: TextWrapper? = null,
    @SerialName("Image") val imageObj: ImageWrapper? = null
)

@Serializable
data class MessageResponseDto(
    @SerialName("id") val id: Long,
    @SerialName("from") val from: String,
    @SerialName("data") val data: MessageDataDto
)
