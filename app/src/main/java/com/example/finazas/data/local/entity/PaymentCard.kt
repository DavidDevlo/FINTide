// data/local/entity/PaymentCard.kt
package com.example.finazas.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cards")
data class PaymentCard(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val holderName: String,                 // Nombre en la tarjeta
    val nickname: String? = null,           // Apodo opcional (p.ej., "Visa sueldo")
    val brand: String,                      // VISA | MASTERCARD | AMEX | DINERS | UNKNOWN
    val panLast4: String,                   // Sólo últimos 4
    val expMonth: Int,                      // 1..12
    val expYear: Int,                       // YYYY
    val colorHex: String = "#3B82F6",
    // UI (azul por defecto)
    val cardType: String = "DEBIT",     // "DEBIT" | "CREDIT"
    val isPhysical: Boolean = true,     // true=física, false=virtual
    val isDefault: Boolean = false,         // Tarjeta principal
    val isActive: Boolean = true,           // Soft-delete
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
