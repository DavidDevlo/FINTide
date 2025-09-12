package com.example.finazas.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,             // Ejemplo: "iPhone 16 Pro"
    val targetAmount: Long,        // Meta en centavos: $20,000.00 -> 2000000
    val currentAmount: Long = 0,   // Progreso actual
    val colorHex: String,          // Guardamos color en formato HEX
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
