package com.afterglow.messenger.data.repository

import com.afterglow.messenger.data.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    /** Prefix search on usernameLower — Firestore has no native "contains". */
    suspend fun searchUsers(query: String, excludeUid: String): List<User> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        val end = q + "\uf8ff"

        val snapshot = firestore.collection("users")
            .orderBy("usernameLower")
            .startAt(q)
            .endAt(end)
            .limit(20)
            .get()
            .await()

        return snapshot.toObjects(User::class.java).filter { it.uid != excludeUid }
    }

    suspend fun getUser(uid: String): User? =
        firestore.collection("users").document(uid).get().await().toObject(User::class.java)
}
