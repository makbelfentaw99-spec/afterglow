package com.afterglow.messenger.ui.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.afterglow.messenger.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException

sealed interface GoogleSignInAttempt {
    data class Success(val idToken: String) : GoogleSignInAttempt
    data object Cancelled : GoogleSignInAttempt
    data class Failed(val message: String) : GoogleSignInAttempt
}

/**
 * The web client ID comes from R.string.default_web_client_id, which the
 * google-services Gradle plugin auto-generates from google-services.json —
 * but ONLY once Google sign-in has been enabled in the Firebase console AND
 * the resulting updated google-services.json has been dropped into app/.
 * Using the old file (from before enabling Google sign-in) means this
 * resource won't exist and the project won't compile.
 */
suspend fun requestGoogleSignIn(context: Context): GoogleSignInAttempt {
    val webClientId = context.getString(R.string.default_web_client_id)

    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(false)
        .setServerClientId(webClientId)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    return try {
        val response = CredentialManager.create(context).getCredential(context, request)
        val credential = response.credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            GoogleSignInAttempt.Success(googleIdTokenCredential.idToken)
        } else {
            GoogleSignInAttempt.Failed("Unexpected sign-in response. Please try again.")
        }
    } catch (e: GetCredentialCancellationException) {
        GoogleSignInAttempt.Cancelled
    } catch (e: GetCredentialException) {
        GoogleSignInAttempt.Failed("Couldn't sign in with Google. Please try again.")
    } catch (e: GoogleIdTokenParsingException) {
        GoogleSignInAttempt.Failed("Couldn't sign in with Google. Please try again.")
    }
}
