package com.credflow.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SessionState(
    val isLoading: Boolean = true,
    val user: FirebaseUser? = null,
    val errorMessage: String = ""
)

class SessionRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val _state = MutableStateFlow(
        SessionState(
            isLoading = auth.currentUser == null,
            user = auth.currentUser
        )
    )
    val state: StateFlow<SessionState> = _state.asStateFlow()

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        _state.value = SessionState(
            isLoading = false,
            user = firebaseAuth.currentUser
        )
    }

    init {
        auth.addAuthStateListener(authStateListener)
        if (auth.currentUser == null) {
            ensureSession()
        }
    }

    fun ensureSession() {
        auth.currentUser?.let { user ->
            _state.value = SessionState(
                isLoading = false,
                user = user
            )
            return
        }

        _state.value = _state.value.copy(
            isLoading = true,
            errorMessage = ""
        )

        auth.signInAnonymously()
            .addOnSuccessListener { result ->
                _state.value = SessionState(
                    isLoading = false,
                    user = result.user
                )
            }
            .addOnFailureListener { exception ->
                _state.value = SessionState(
                    isLoading = false,
                    errorMessage = exception.localizedMessage
                        ?: "Unable to start the app session."
                )
            }
    }
}
