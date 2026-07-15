package com.afterglow.messenger.data.repository

import com.afterglow.messenger.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

// Firebase Auth needs an email, but this app only asks people for a
// username. We derive a stable, never-shown synthetic email from the
// username so Auth's battle-tested password handling can be reused without
// collecting real email addresses. (Trade-off: no email-based password
// reset. Add real emails later if you want that.)
private const val EMAIL_DOMAIN = "afterglow.internal"
private val USERNAME_REGEX = Regex("^[a-zA-Z0-9_]{3,20}$")

class UsernameTakenException :
    Exception("Username already taken. Please choose another one.")

/**
 * [user] is null when [needsUsername] is true — Google gives us an email
 * and a display name, never a chosen username, so first-time Google
 * sign-ins land here instead of a finished profile.
 */
data class GoogleSignInOutcome(
    val uid: String,
    val user: User?,
    val needsUsername: Boolean
)

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    val currentUserId: String? get() = auth.currentUser?.uid
    val isSignedIn: Boolean get() = auth.currentUser != null

    /**
     * Order of operations matters here: we create the Firebase Auth account
     * FIRST (so the caller is authenticated), then claim the username in
     * Firestore. Firestore security rules require sign-in to write, so
     * trying to reserve the username before creating the account would be
     * rejected. If the username turns out to be taken by the time we get to
     * the Firestore transaction (a rare race), we roll back by deleting the
     * Auth account we just created.
     */
    suspend fun register(username: String, displayName: String, password: String): Result<User> {
        if (!username.matches(USERNAME_REGEX)) {
            return Result.failure(
                Exception("Usernames must be 3-20 characters: letters, numbers, and underscores only.")
            )
        }
        val usernameLower = username.lowercase()
        val email = "$usernameLower@$EMAIL_DOMAIN"

        val authResult = try {
            auth.createUserWithEmailAndPassword(email, password).await()
        } catch (e: FirebaseAuthUserCollisionException) {
            return Result.failure(UsernameTakenException())
        } catch (e: FirebaseAuthWeakPasswordException) {
            return Result.failure(Exception("Password is too weak. Use at least 6 characters."))
        } catch (e: Exception) {
            return Result.failure(Exception("Couldn't create account. Please try again."))
        }

        val uid = authResult.user?.uid
            ?: return Result.failure(Exception("Couldn't create account. Please try again."))

        val usernameRef = firestore.collection("usernames").document(usernameLower)
        val reserved = try {
            firestore.runTransaction { txn ->
                val snapshot = txn.get(usernameRef)
                if (snapshot.exists()) {
                    false
                } else {
                    txn.set(usernameRef, mapOf("uid" to uid))
                    true
                }
            }.await()
        } catch (e: Exception) {
            false
        }

        if (!reserved) {
            auth.currentUser?.delete()?.await()
            return Result.failure(UsernameTakenException())
        }

        val user = User(
            uid = uid,
            username = username,
            usernameLower = usernameLower,
            displayName = displayName.ifBlank { username },
            createdAt = System.currentTimeMillis()
        )

        return try {
            firestore.collection("users").document(uid).set(user).await()
            Result.success(user)
        } catch (e: Exception) {
            usernameRef.delete().await()
            auth.currentUser?.delete()?.await()
            Result.failure(Exception("Couldn't finish setting up your account. Please try again."))
        }
    }

    suspend fun login(username: String, password: String): Result<Unit> {
        val email = "${username.trim().lowercase()}@$EMAIL_DOMAIN"
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            Result.success(Unit)
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Result.failure(Exception("Incorrect username or password."))
        } catch (e: Exception) {
            Result.failure(Exception("Couldn't sign in. Check your username and password."))
        }
    }

    /**
     * Exchanges a Google ID token (from Credential Manager) for a Firebase
     * session. Firebase creates the Auth account automatically on first
     * sign-in — there's no separate "register" step for Google users, just
     * a check afterward for whether they've picked a username yet.
     */
    suspend fun signInWithGoogleIdToken(idToken: String): Result<GoogleSignInOutcome> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val uid = authResult.user?.uid
                ?: return Result.failure(Exception("Google sign-in failed. Please try again."))

            val existing = firestore.collection("users").document(uid).get().await()
                .toObject(User::class.java)

            Result.success(
                GoogleSignInOutcome(uid = uid, user = existing, needsUsername = existing == null)
            )
        } catch (e: Exception) {
            Result.failure(Exception("Google sign-in failed. Please try again."))
        }
    }

    /**
     * Finishes account setup for a user who signed in with Google but
     * doesn't have a username yet. Doesn't touch the Auth account on
     * failure (unlike register()) — they're already validly signed in
     * either way and can just retry.
     */
    suspend fun claimUsername(uid: String, username: String, displayName: String): Result<User> {
        if (!username.matches(USERNAME_REGEX)) {
            return Result.failure(
                Exception("Usernames must be 3-20 characters: letters, numbers, and underscores only.")
            )
        }
        val usernameLower = username.lowercase()
        val usernameRef = firestore.collection("usernames").document(usernameLower)

        val reserved = try {
            firestore.runTransaction { txn ->
                val snapshot = txn.get(usernameRef)
                if (snapshot.exists()) {
                    false
                } else {
                    txn.set(usernameRef, mapOf("uid" to uid))
                    true
                }
            }.await()
        } catch (e: Exception) {
            false
        }

        if (!reserved) {
            return Result.failure(UsernameTakenException())
        }

        val user = User(
            uid = uid,
            username = username,
            usernameLower = usernameLower,
            displayName = displayName.ifBlank { username },
            createdAt = System.currentTimeMillis()
        )

        return try {
            firestore.collection("users").document(uid).set(user).await()
            Result.success(user)
        } catch (e: Exception) {
            usernameRef.delete().await()
            Result.failure(Exception("Couldn't finish setting up your account. Please try again."))
        }
    }

    fun logout() = auth.signOut()
}
