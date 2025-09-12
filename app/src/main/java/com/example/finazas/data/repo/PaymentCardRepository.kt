package com.example.finazas.data.repo

import com.example.finazas.data.local.dao.PaymentCardDao
import com.example.finazas.data.local.entity.PaymentCard
import kotlinx.coroutines.flow.Flow

class PaymentCardRepository(private val dao: PaymentCardDao) {

    fun observeActive(): Flow<List<PaymentCard>> = dao.observeActive()
    fun search(q: String): Flow<List<PaymentCard>> = dao.search(q)
    suspend fun get(id: Long): PaymentCard? = dao.findById(id)

    suspend fun create(
        holderName: String,
        nickname: String?,
        brand: String,
        panLast4: String,
        expMonth: Int,
        expYear: Int,
        colorHex: String,
        isDefault: Boolean,
        cardType: String,          // NUEVO
        isPhysical: Boolean        // NUEVO
    ): Long {
        val now = System.currentTimeMillis()
        val card = PaymentCard(
            holderName = holderName.trim(),
            nickname = nickname?.trim(),
            brand = brand.trim().uppercase(),
            panLast4 = panLast4.trim(),
            expMonth = expMonth,
            expYear = expYear,
            colorHex = colorHex.trim(),
            cardType = cardType.trim().uppercase(),
            isPhysical = isPhysical,
            isDefault = isDefault,
            createdAt = now,
            updatedAt = now
        )
        val id = dao.insert(card)
        if (isDefault) dao.setDefaultExclusive(id)
        return id
    }

    suspend fun updateMeta(
        id: Long,
        nickname: String?,
        colorHex: String,
        setDefault: Boolean
    ) {
        val existing = dao.findById(id) ?: return
        val updated = existing.copy(
            nickname = nickname?.trim(),
            colorHex = colorHex.trim(),
            updatedAt = System.currentTimeMillis()
        )
        dao.updateRaw(updated)
        if (setDefault) dao.setDefaultExclusive(id)
    }

    suspend fun delete(id: Long) = dao.softDelete(id)
    suspend fun setDefault(id: Long) = dao.setDefaultExclusive(id)
}
