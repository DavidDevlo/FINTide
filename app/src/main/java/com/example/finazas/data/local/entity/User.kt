package com.example.finazas.data.local.entity

// package com.fintide.app.data
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val givenName: String,
    val familyName: String,
    val email: String,
    val avatarUrl: String? = null,
    val provider: String = "MANUAL",   // "MANUAL" | "GOOGLE" | "FACEBOOK" | "TWITTER"
    val providerUid: String? = null,

    // PIN seguro: guardamos hash + salt (NO el PIN en texto plano)
    val pinHash: String,
    val pinSalt: String,

    // Estado de onboarding y sesión
    val isActive: Boolean = true,
    val isOnboarded: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
