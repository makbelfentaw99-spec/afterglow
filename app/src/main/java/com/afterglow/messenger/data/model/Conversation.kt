package com.afterglow.messenger.data.model

/**
 * A 1:1 conversation. [id] is deterministic (sorted uid pair, see
 * ChatRepository.conversationId) so re-opening a chat with the same person
 * always resolves to the same document instead of creating duplicates.
 */
data class Conversation(
    val id: String = "",
    val participants: List<String> = emptyList(),
    val participantUsernames: Map<String, String> = emptyMap(),
    val lastMessageText: String = "",
    val lastMessageTimestamp: Long = 0L,
    val lastMessageSenderId: String = "",
    val createdAt: Long = 0L
) {
    fun otherParticipantId(myUid: String): String =
        participants.firstOrNull { it != myUid } ?: ""

    fun otherParticipantUsername(myUid: String): String =
        participantUsernames[otherParticipantId(myUid)] ?: "Unknown"
}
