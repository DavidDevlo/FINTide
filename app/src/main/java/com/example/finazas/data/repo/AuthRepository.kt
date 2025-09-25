package com.example.finazas.data.repo

// package com.fintide.app.auth
import com.example.finazas.data.local.dao.UserDao
import com.example.finazas.data.local.entity.User
import java.security.MessageDigest
import kotlin.random.Random

class AuthRepository(private val userDao: UserDao) {

    suspend fun getActiveUser(): User? = userDao.getActiveUser()

    suspend fun completeOnboarding() {
        val u = userDao.getActiveUser() ?: return
        userDao.update(u.copy(isOnboarded = true))
    }

    suspend fun signInManual(
        givenName: String,
        familyName: String,
        email: String,
    ): User {
        userDao.deactivateAll()
        val placeholderPin = "000000" // temporal hasta que lo cambie en CreatePin
        val salt = newSalt()
        val hash = hashPin(placeholderPin, salt)
        val user = User(
            givenName = givenName,
            familyName = familyName,
            email = email,
            provider = "MANUAL",
            pinHash = hash,
            pinSalt = salt,
            isActive = true
        )
        val id = userDao.insert(user)
        return user.copy(id = id)
    }

    suspend fun signInSocial(
        provider: String,            // "GOOGLE" | "FACEBOOK" | "TWITTER"
        providerUid: String,
        givenName: String,
        familyName: String,
        email: String,
        avatarUrl: String?
    ): User {
        userDao.deactivateAll()
        val salt = newSalt()
        val hash = hashPin("000000", salt) // pediremos nuevo PIN en CreatePin
        val user = User(
            givenName = givenName,
            familyName = familyName,
            email = email,
            avatarUrl = avatarUrl,
            provider = provider,
            providerUid = providerUid,
            pinHash = hash,
            pinSalt = salt,
            isActive = true
        )
        val id = userDao.insert(user)
        return user.copy(id = id)
    }

    suspend fun setNewPin(pin6: String) {
        require(pin6.length == 6 && pin6.all { it.isDigit() })
        val u = userDao.getActiveUser() ?: return
        val salt = newSalt()
        val hash = hashPin(pin6, salt)
        userDao.update(u.copy(pinSalt = salt, pinHash = hash))
    }

    suspend fun verifyPin(pin6: String): Boolean {
        val u = userDao.getActiveUser() ?: return false
        return u.pinHash == hashPin(pin6, u.pinSalt)
    }

    private fun newSalt(): String = List(16) { ('a'..'z').random() }.joinToString("")

    private fun hashPin(pin: String, salt: String): String {
        val bytes = (salt + pin).encodeToByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }
}
