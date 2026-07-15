package com.afterglow.messenger.data.model

/**
 * MessageType already lists Phase 2/3 kinds (stickers, voice, image, video)
 * even though only TEXT is wired up yet, so the schema doesn't need to
 * change shape when those land.
 */
enum class MessageType { TEXT, STICKER, IMAGE, VIDEO, VOICE }

enum class MessageStatus { SENT, DELIVERED, READ }

data class Message(
    val id: String = "",
    val conversationId: String = "",
    val senderId: String = "",
    val text: String = "",
    val type: MessageType = MessageType.TEXT,
    val mediaUrl: String? = null,
    val timestamp: Long = 0L,
    val status: MessageStatus = MessageStatus.SENT
)
