package com.credflow.data.profile

import com.credflow.data.auth.LocalIdentityRepository
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

data class UserProfile(
    val uid: String = "",
    val displayName: String = "",
    val businessName: String = "",
    val email: String = "",
    val isProfileComplete: Boolean = false
)

data class UserProfileState(
    val isLoading: Boolean = false,
    val profile: UserProfile? = null
)

class UserProfileRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val _state = MutableStateFlow(UserProfileState(isLoading = true))
    val state: StateFlow<UserProfileState> = _state.asStateFlow()

    private var registration: ListenerRegistration? = null

    fun observeCurrentUserProfile() {
        registration?.remove()
        val userId = LocalIdentityRepository.userId()

        _state.value = UserProfileState(isLoading = true)
        registration = profileDocument(userId).addSnapshotListener { snapshot, _ ->
            val profile = if (snapshot == null || !snapshot.exists()) {
                UserProfile(uid = userId)
            } else {
                UserProfile(
                    uid = userId,
                    displayName = snapshot.getString("displayName").orEmpty(),
                    businessName = snapshot.getString("businessName").orEmpty(),
                    email = snapshot.getString("email").orEmpty(),
                    isProfileComplete = snapshot.getBoolean("isProfileComplete") == true
                )
            }

            _state.value = UserProfileState(
                isLoading = false,
                profile = profile
            )
        }
    }

    suspend fun saveProfile(
        displayName: String,
        businessName: String,
        email: String
    ) {
        val userId = LocalIdentityRepository.userId()
        profileDocument(userId)
            .set(
                mapOf(
                    "uid" to userId,
                    "phoneNumber" to FieldValue.delete(),
                    "displayName" to displayName.trim(),
                    "businessName" to businessName.trim(),
                    "email" to email.trim(),
                    "isProfileComplete" to true
                ),
                SetOptions.merge()
            )
            .await()
    }

    suspend fun exportProfileMap(): Map<String, Any> {
        val snapshot = profileDocument(LocalIdentityRepository.userId()).get().await()
        return snapshot.data.orEmpty()
            .filterKeys { it != "phoneNumber" }
    }

    suspend fun restoreProfileMap(data: Map<String, Any>) {
        val sanitizedData = data
            .filterKeys { it != "phoneNumber" }
            .toMutableMap<String, Any>()
        sanitizedData["phoneNumber"] = FieldValue.delete()

        profileDocument(LocalIdentityRepository.userId())
            .set(sanitizedData, SetOptions.merge())
            .await()
    }

    private fun profileDocument(uid: String) = db.collection("users")
        .document(uid)
        .collection("profile")
        .document("main")
}
