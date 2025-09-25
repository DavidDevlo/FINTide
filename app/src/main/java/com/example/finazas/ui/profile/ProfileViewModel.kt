package com.example.finazas.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.finazas.data.local.dao.UserDao
import com.example.finazas.data.local.entity.User
import com.example.finazas.data.repo.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileUi(
    val username: String = "",
    val name: String = "",
    val lastName: String = "",
    val email: String = "",
    val loading: Boolean = true,
    val error: String? = null,
    val message: String? = null
)

class ProfileViewModel(
    private val userDao: UserDao,
    private val authRepo: AuthRepository
) : ViewModel() {

    private var currentUser: User? = null
    private val _ui = MutableStateFlow(ProfileUi())
    val ui = _ui.asStateFlow()

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val u = userDao.getActiveUser()
        currentUser = u
        if (u == null) {
            _ui.value = ProfileUi(loading = false, error = "No hay usuario activo.")
            return
        }
        _ui.value = ProfileUi(
            username = buildUsername(u),
            name = u.givenName,
            lastName = u.familyName,
            email = u.email,
            loading = false
        )
    }

    private fun buildUsername(u: User): String {
        val joined = (u.givenName + u.familyName).lowercase()
        return if (joined.isNotBlank()) joined else u.email.substringBefore('@')
    }

    private fun applyAndRefresh(transform: (User) -> User, successMsg: String) {
        viewModelScope.launch {
            val u = currentUser ?: return@launch
            val updated = transform(u)
            userDao.update(updated)
            currentUser = updated
            _ui.value = _ui.value.copy(
                username = buildUsername(updated),
                name = updated.givenName,
                lastName = updated.familyName,
                email = updated.email,
                message = successMsg,
                error = null
            )
        }
    }

    fun updateFullName(newName: String, newLastName: String) {
        applyAndRefresh(
            transform = { it.copy(givenName = newName, familyName = newLastName) },
            successMsg = "Nombre actualizado"
        )
    }

    fun updateName(newName: String) {
        applyAndRefresh(
            transform = { it.copy(givenName = newName) },
            successMsg = "Nombre actualizado"
        )
    }

    fun updateLastName(newLast: String) {
        applyAndRefresh(
            transform = { it.copy(familyName = newLast) },
            successMsg = "Apellido actualizado"
        )
    }

    fun updateEmail(newEmail: String) {
        applyAndRefresh(
            transform = { it.copy(email = newEmail) },
            successMsg = "Correo actualizado"
        )
    }

    fun changePin(pin1: String, pin2: String) {
        if (pin1.length != 6 || !pin1.all { it.isDigit() }) {
            _ui.value = _ui.value.copy(error = "El PIN debe tener 6 dígitos.")
            return
        }
        if (pin1 != pin2) {
            _ui.value = _ui.value.copy(error = "Los PIN no coinciden.")
            return
        }
        viewModelScope.launch {
            authRepo.setNewPin(pin1)
            _ui.value = _ui.value.copy(message = "PIN actualizado", error = null)
        }
    }

    fun clearFeedback() {
        _ui.value = _ui.value.copy(message = null, error = null)
    }
}
