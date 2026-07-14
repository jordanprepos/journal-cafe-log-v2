package com.example.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GoogleUser(
    val id: String,
    val email: String,
    val displayName: String,
    val photoUrl: String?
)

class AuthViewModel(context: Context) : ViewModel() {
    private val sharedPrefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    
    private val _userState = MutableStateFlow<GoogleUser?>(null)
    val userState: StateFlow<GoogleUser?> = _userState.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        // Restore session from preferences if saved
        val savedEmail = sharedPrefs.getString("email", null)
        val savedId = sharedPrefs.getString("id", null)
        val savedName = sharedPrefs.getString("name", null)
        val savedPhoto = sharedPrefs.getString("photo", null)
        
        if (savedEmail != null && savedId != null) {
            _userState.value = GoogleUser(
                id = savedId,
                email = savedEmail,
                displayName = savedName ?: "Cafe Lover",
                photoUrl = savedPhoto
            )
        }
    }

    fun loginWithGoogle(context: Context, onAuthSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _authError.value = null
            
            try {
                val credentialManager = CredentialManager.create(context)
                
                // Set up Google ID Option
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId("dummy_client_id.apps.googleusercontent.com") // Under real circumstances, user replaces with actual client ID
                    .setAutoSelectEnabled(true)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(context, request)
                val credential = result.credential

                if (credential is GoogleIdTokenCredential) {
                    val googleIdTokenCredential = credential
                    val email = googleIdTokenCredential.id
                    val displayName = googleIdTokenCredential.displayName ?: "Cafe Traveler"
                    val photoUri = googleIdTokenCredential.profilePictureUri?.toString()
                    
                    val user = GoogleUser(
                        id = googleIdTokenCredential.id,
                        email = email,
                        displayName = displayName,
                        photoUrl = photoUri
                    )
                    
                    saveUserSession(user)
                    _userState.value = user
                    onAuthSuccess()
                } else {
                    _authError.value = "Unexpected credential type returned."
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Google Login failed: ${e.message}", e)
                _authError.value = e.localizedMessage ?: "Google Sign-In failed."
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Direct developer fallback for testing on emulators or where Play Services is not available
    fun loginWithTestAccount(email: String, name: String, photoUrl: String?, onAuthSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _authError.value = null
            
            val user = GoogleUser(
                id = "test_user_id_${email.hashCode()}",
                email = email,
                displayName = name,
                photoUrl = photoUrl
            )
            
            saveUserSession(user)
            _userState.value = user
            onAuthSuccess()
            _isLoading.value = false
        }
    }

    fun logout(onSuccess: () -> Unit) {
        sharedPrefs.edit().clear().apply()
        _userState.value = null
        onSuccess()
    }

    private fun saveUserSession(user: GoogleUser) {
        sharedPrefs.edit().apply {
            putString("id", user.id)
            putString("email", user.email)
            putString("name", user.displayName)
            putString("photo", user.photoUrl)
            apply()
        }
    }
}
