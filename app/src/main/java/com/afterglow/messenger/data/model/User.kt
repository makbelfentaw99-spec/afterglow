package com.afterglow.messenger.data.model

/**
 * A registered account. [usernameLower] exists purely so Firestore range
 * queries (used for search) can stay case-insensitive.
 */
data class User(
    val uid: String = "",
    val username: String = "",
    val usernameLower: String = "",
    val displayName: String = "",
    val createdAt: Long = 0L
)
