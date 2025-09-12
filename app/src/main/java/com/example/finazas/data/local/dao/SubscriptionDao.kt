// data/local/dao/SubscriptionDao.kt
package com.example.finazas.data.local.dao

import androidx.room.*
import com.example.finazas.data.local.entity.Subscription
import kotlinx.coroutines.flow.Flow


@Dao
interface SubscriptionDao {
    @Query("SELECT * FROM subscriptions ORDER BY nextDueAt ASC")
    fun observeAll(): Flow<List<Subscription>>

    @Query("SELECT * FROM subscriptions WHERE title LIKE '%' || :q || '%' ORDER BY nextDueAt ASC")
    fun search(q: String): Flow<List<Subscription>>

    @Query("SELECT * FROM subscriptions WHERE nextDueAt BETWEEN :start AND :end AND isActive = 1 ORDER BY nextDueAt ASC")
    fun observeDueBetween(start: Long, end: Long): Flow<List<Subscription>>

    @Query("SELECT * FROM subscriptions WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): Subscription?

    @Insert
    suspend fun insert(s: Subscription): Long

    /** Tu repo llama updateRaw(s.copy(...)) */
    @Update
    suspend fun updateRaw(s: Subscription)

    /** setCoreFields(...) sin pasar ts explícito: usamos default param */
    @Query("""
        UPDATE subscriptions SET
            title = :title,
            amountCents = :amountCents,
            variableAmount = :variableAmount,
            frequency = :frequency,
            intervalDays = :intervalDays,
            nextDueAt = :nextDueAt,
            autoPay = :autoPay,
            colorHex = :colorHex,
            updatedAt = :ts
        WHERE id = :id
    """)
    suspend fun setCoreFields(
        id: Long,
        title: String,
        amountCents: Long?,
        variableAmount: Boolean,
        frequency: String,
        intervalDays: Int?,
        nextDueAt: Long,
        autoPay: Boolean,
        colorHex: String,
        ts: Long = System.currentTimeMillis()
    )

    /** Tu repo llama softDelete(id) */
    @Query("UPDATE subscriptions SET isActive = 0, updatedAt = :ts WHERE id = :id")
    suspend fun softDelete(id: Long, ts: Long = System.currentTimeMillis())

    /** Tu repo llama markPaidInternal(id, amount, paidAt, next) */
    @Query("""
        UPDATE subscriptions SET
            lastPaidAt = :paidAt,
            lastPaidAmountCents = :amount,
            nextDueAt = :nextDueAt,
            updatedAt = :paidAt
        WHERE id = :id
    """)
    suspend fun markPaidInternal(
        id: Long,
        amount: Long,
        paidAt: Long,
        nextDueAt: Long
    )
}
