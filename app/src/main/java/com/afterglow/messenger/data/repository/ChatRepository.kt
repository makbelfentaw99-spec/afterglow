package com.afterglow.messenger.data.repository

import com.afterglow.messenger.data.model.Conversation
import com.afterglow.messenger.data.model.Message
import com.afterglow.messenger.data.model.MessageStatus
import com.afterglow.messenger.data.model.MessageType
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ChatRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    /** Sorted pair so re-opening a chat with the same person is idempotent. */
    private fun conversationId(uidA: String, uidB: String): String =
        if (uidA < uidB) "${uidA}_$uidB" else "${uidB}_$uidA"

    suspend fun getOrCreateConversation(
        myUid: String,
        myUsername: String,
        otherUid: String,
        otherUsername: String
    ): Conversation {
        val id = conversationId(myUid, otherUid)
        val ref = firestore.collection("conversations").document(id)
        val existing = ref.get().await()
        if (existing.exists()) {
            return existing.toObject(Conversation::class.java)!!.copy(id = id)
        }

        val conversation = Conversation(
            id = id,
            participants = listOf(myUid, otherUid),
            participantUsernames = mapOf(myUid to myUsername, otherUid to otherUsername),
            lastMessageText = "",
            lastMessageTimestamp = System.currentTimeMillis(),
            lastMessageSenderId = "",
            createdAt = System.currentTimeMillis()
        )
        ref.set(conversation).await()
        return conversation
    }

    fun observeConversations(myUid: String): Flow<List<Conversation>> = callbackFlow {
        val listener = firestore.collection("conversations")
            .whereArrayContains("participants", myUid)
            .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val conversations = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Conversation::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(conversations)
            }
        awaitClose { listener.remove() }
    }

    fun observeMessages(conversationId: String): Flow<List<Message>> = callbackFlow {
        val listener = firestore.collection("conversations").document(conversationId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Message::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(messages)
            }
        awaitClose { listener.remove() }
    }

    suspend fun sendTextMessage(conversationId: String, senderId: String, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        val conversationRef = firestore.collection("conversations").document(conversationId)
        val messageRef = conversationRef.collection("messages").document()
        val timestamp = System.currentTimeMillis()

        val message = Message(
            id = messageRef.id,
            conversationId = conversationId,
            senderId = senderId,
            text = trimmed,
            type = MessageType.TEXT,
            timestamp = timestamp,
            status = MessageStatus.SENT
        )

        firestore.batch().apply {
            set(messageRef, message)
            update(
                conversationRef,
                mapOf(
                    "lastMessageText" to trimmed,
                    "lastMessageTimestamp" to timestamp,
                    "lastMessageSenderId" to senderId
                )
            )
        }.commit().await()
    }
}
