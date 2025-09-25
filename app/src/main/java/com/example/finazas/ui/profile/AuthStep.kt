package com.example.finazas.ui.profile

// package com.fintide.app.auth
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.finazas.data.repo.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AuthStep {
    data object Onboarding : AuthStep
    data object Login : AuthStep
    data object CreatePin : AuthStep
    data object Unlock : AuthStep
    data object Done : AuthStep
}

data class AuthUiState(
    val step: AuthStep = AuthStep.Login,
    val loading: Boolean = false,
    val error: String? = null
)

class AuthViewModel(private val repo: AuthRepository) : ViewModel() {
    private val _ui = MutableStateFlow(AuthUiState(step = AuthStep.Onboarding))
    val ui = _ui.asStateFlow()

    init { viewModelScope.launch { decideStart() } }

    private suspend fun decideStart() {
        val u = repo.getActiveUser()
        _ui.value = when {
            u == null -> AuthUiState(step = AuthStep.Onboarding)
            !u.isOnboarded -> AuthUiState(step = AuthStep.Unlock) // usuario ya existe: pedir PIN
            else -> AuthUiState(step = AuthStep.Unlock)
        }
    }

    /** Termina el Onboarding inicial y pasa a Login (crear cuenta) */
    fun onOnboardingSeen() {
        _ui.value = _ui.value.copy(step = AuthStep.Login, error = null)
    }

    fun onManualRegister(given: String, family: String, email: String) {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(loading = true, error = null)
            try {
                repo.signInManual(given, family, email)
                _ui.value = _ui.value.copy(step = AuthStep.CreatePin, loading = false)
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(loading = false, error = e.message)
            }
        }
    }

    /** Llamar esto tras Google Sign-In real (ver abajo) */
    fun onGoogleSignedIn(
        sub: String,
        givenName: String?,
        familyName: String?,
        email: String,
        picture: String?
    ) {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(loading = true, error = null)
            repo.signInSocial(
                provider = "GOOGLE",
                providerUid = sub,
                givenName = givenName ?: "",
                familyName = familyName ?: "",
                email = email,
                avatarUrl = picture
            )
            _ui.value = _ui.value.copy(step = AuthStep.CreatePin, loading = false)
        }
    }

    fun onSetPin(pin6: String) {
        viewModelScope.launch {
            repo.setNewPin(pin6)
            // Marcamos Onboarding como completado ahora que ya existe un usuario y PIN
            repo.completeOnboarding()
            _ui.value = _ui.value.copy(step = AuthStep.Done)
        }
    }

    fun onUnlock(pin6: String) {
        viewModelScope.launch {
            val ok = repo.verifyPin(pin6)
            _ui.value = if (ok) _ui.value.copy(step = AuthStep.Done, error = null)
            else _ui.value.copy(error = "PIN incorrecto")
        }
    }
}
