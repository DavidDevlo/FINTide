package com.example.finazas.data.local.entity


import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "movements")
data class Movement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,               // Ejemplo: "Spotify Premium"
    val type: String,                // "Ingreso" o "Egreso"
    val amount: Long,                // En centavos
    val date: Long = System.currentTimeMillis(),
    val stripeColorHex: String = "#22C55E", // Color para la franja izquierda
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
