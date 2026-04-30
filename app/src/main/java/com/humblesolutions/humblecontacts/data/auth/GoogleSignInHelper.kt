// data/auth/GoogleSignInHelper.kt
package com.humblesolutions.humblecontacts.data.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

// This helper owns everything related to launching Google Sign-In
// and extracting the idToken from the result.
// It's separate from AuthRepository because it needs an Activity context
// (CredentialManager must be launched from an Activity, not just any Context).
class GoogleSignInHelper(private val context: Context) {

    // CredentialManager is the new unified API for all credential types
    // (passwords, passkeys, federated identity like Google).
    private val credentialManager = CredentialManager.create(context)

    // Sealed class so callers can pattern-match the result cleanly
    sealed class GoogleSignInResult {
        data class Success(val idToken: String) : GoogleSignInResult()
        data class Error(val message: String)   : GoogleSignInResult()
        object Cancelled                         : GoogleSignInResult()
    }

    suspend fun signIn(): GoogleSignInResult {
        return try {
            // GetGoogleIdOption configures what we request from Google.
            val googleIdOption = GetGoogleIdOption.Builder()
                // Your Web Client ID from google-services.json
                // This is auto-generated — find it in R.string.default_web_client_id
                .setServerClientId(context.getString(com.humblesolutions.humblecontacts.R.string.default_web_client_id))

                // Show ALL accounts, not just previously authorized ones.
                .setFilterByAuthorizedAccounts(false)

                // Always show the picker — never auto-select.
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            // launch() suspends until the user picks an account or dismisses.
            // Must be called from an Activity context — hence we need the Activity.
            val result = credentialManager.getCredential(
                request = request,
                context = context
            )

            // result.credential can be of different types (password, passkey, google, etc.)
            // We only care about GoogleIdTokenCredential.
            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                // Parse the credential into a GoogleIdTokenCredential object
                val googleCred = GoogleIdTokenCredential.createFrom(credential.data)
                // idToken is the JWT we pass to Firebase
                GoogleSignInResult.Success(googleCred.idToken)
            } else {
                GoogleSignInResult.Error("Unexpected credential type")
            }

        } catch (e: GetCredentialCancellationException) {
            // User pressed back or dismissed the picker — not an error, just cancelled
            GoogleSignInResult.Cancelled

        } catch (e: androidx.credentials.exceptions.NoCredentialException) {
            // No previously authorized accounts found.
            // Fall back to showing ALL Google accounts on the device.
            signInWithAllAccounts()

        } catch (e: Exception) {
            GoogleSignInResult.Error(e.localizedMessage ?: "Google Sign-In failed")
        }
    }

    // Fallback: show all Google accounts (for first-time users)
    private suspend fun signInWithAllAccounts(): GoogleSignInResult {
        return try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setServerClientId(context.getString(com.humblesolutions.humblecontacts.R.string.default_web_client_id))
                .setFilterByAuthorizedAccounts(false)  // ← show ALL accounts
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(request = request, context = context)
            val credential = result.credential

            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleCred = GoogleIdTokenCredential.createFrom(credential.data)
                GoogleSignInResult.Success(googleCred.idToken)
            } else {
                GoogleSignInResult.Error("Unexpected credential type")
            }
        } catch (e: GetCredentialCancellationException) {
            GoogleSignInResult.Cancelled
        } catch (e: Exception) {
            GoogleSignInResult.Error(e.localizedMessage ?: "Google Sign-In failed")
        }
    }
}