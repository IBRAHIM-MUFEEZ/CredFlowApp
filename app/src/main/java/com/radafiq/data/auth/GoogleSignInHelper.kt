package com.radafiq.data.auth

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object GoogleSignInHelper {

    const val DRIVE_APPDATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
    private val OAUTH_SCOPE = "oauth2:$DRIVE_APPDATA_SCOPE"

    // Web client ID (client_type 3) from google-services.json
    private const val WEB_CLIENT_ID =
        "1036438568871-h04rt7h9gqqcmfiodi3liku0bjll1gm7.apps.googleusercontent.com"

    fun buildClient(context: Context): GoogleSignInClient {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(WEB_CLIENT_ID)
            .requestEmail()
            .requestScopes(Scope(DRIVE_APPDATA_SCOPE))
            .build()
        return GoogleSignIn.getClient(context, options)
    }

    fun signInIntent(context: Context): Intent = buildClient(context).signInIntent

    /** Returns the cached account only if the drive.appdata scope is already granted. */
    fun getSignedInAccount(context: Context): GoogleSignInAccount? {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return null
        return if (GoogleSignIn.hasPermissions(account, Scope(DRIVE_APPDATA_SCOPE))) account else null
    }

    /**
     * Returns the last signed-in account regardless of scope check.
     * Use for auto-backup where scope was already granted during sign-in.
     */
    fun getLastSignedInAccount(context: Context): GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(context)
    }

    /**
     * Signs into Firebase Auth using the Google ID token from the signed-in account.
     * Returns the Firebase UID on success, null on failure.
     *
     * Call this after a successful Google Sign-In result before doing anything else.
     */
    suspend fun signInToFirebase(account: GoogleSignInAccount): String? {
        val idToken = account.idToken
        if (idToken.isNullOrBlank()) {
            android.util.Log.w("FirebaseAuth", "No ID token — requestIdToken() may not be configured")
            return null
        }
        return runCatching {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = FirebaseAuth.getInstance().signInWithCredential(credential).await()
            val uid = result.user?.uid
            android.util.Log.d("FirebaseAuth", "Signed in — UID: $uid")
            uid
        }.onFailure { e ->
            android.util.Log.w("FirebaseAuth", "Firebase sign-in failed: ${e.localizedMessage}", e)
        }.getOrNull()
    }

    /**
     * Fetches a fresh OAuth Bearer token for the Drive appdata scope.
     * Must be called from a coroutine — runs on IO dispatcher internally.
     */
    suspend fun fetchAccessToken(context: Context, account: GoogleSignInAccount): String =
        withContext(Dispatchers.IO) {
            GoogleAuthUtil.getToken(
                context,
                account.account ?: error("No Google account attached"),
                OAUTH_SCOPE
            )
        }

    fun signOut(context: Context) {
        FirebaseAuth.getInstance().signOut()
        buildClient(context).signOut()
    }
}
