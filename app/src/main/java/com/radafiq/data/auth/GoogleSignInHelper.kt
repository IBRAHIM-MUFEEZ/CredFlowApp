package com.radafiq.data.auth

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
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
        buildClient(context).signOut()
    }
}
