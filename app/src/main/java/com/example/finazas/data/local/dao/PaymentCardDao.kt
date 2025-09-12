package com.example.finazas.data.local.dao

import androidx.room.*
import com.example.finazas.data.local.entity.PaymentCard
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentCardDao {

    @Query("""
        SELECT * FROM cards 
        WHERE isActive = 1 
        ORDER BY isDefault DESC, updatedAt DESC
    """)
    fun observeActive(): Flow<List<PaymentCard>>

    @Query("""
        SELECT * FROM cards 
        WHERE isActive = 1 
          AND (holderName LIKE '%'||:q||'%' OR nickname LIKE '%'||:q||'%' OR brand LIKE '%'||:q||'%' OR panLast4 LIKE '%'||:q||'%')
        ORDER BY isDefault DESC, updatedAt DESC
    """)
    fun search(q: String): Flow<List<PaymentCard>>

    @Query("SELECT * FROM cards WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): PaymentCard?

    @Insert
    suspend fun insert(card: PaymentCard): Long

    @Update
    suspend fun updateRaw(card: PaymentCard)

    @Query("UPDATE cards SET isActive = 0, updatedAt = :ts WHERE id = :id")
    suspend fun softDelete(id: Long, ts: Long = System.currentTimeMillis())

    // --- Default exclusiva ---
    @Query("UPDATE cards SET isDefault = 0")
    suspend fun clearDefault()

    @Query("UPDATE cards SET isDefault = 1, updatedAt = :ts WHERE id = :id")
    suspend fun setDefaultInternal(id: Long, ts: Long = System.currentTimeMillis())

    @Transaction
    suspend fun setDefaultExclusive(id: Long) {
        clearDefault()
        setDefaultInternal(id)
    }
}
