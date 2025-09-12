package com.example.finazas.data.local.dao


import androidx.room.*
import com.example.finazas.data.local.entity.Movement
import kotlinx.coroutines.flow.Flow

@Dao
interface MovementDao {

    @Query("SELECT * FROM movements WHERE isActive = 1 ORDER BY date DESC, updatedAt DESC")
    fun observeAll(): Flow<List<Movement>>

    @Query("""
        SELECT * FROM movements 
        WHERE isActive = 1 AND title LIKE '%' || :q || '%'
        ORDER BY date DESC, updatedAt DESC
    """)
    fun search(q: String): Flow<List<Movement>>

    @Query("""
        SELECT * FROM movements
        WHERE isActive = 1 AND type = :type
        ORDER BY date DESC, updatedAt DESC
    """)
    fun observeByType(type: String): Flow<List<Movement>>

    @Query("""
        SELECT * FROM movements
        WHERE isActive = 1 AND date BETWEEN :start AND :end
        ORDER BY date DESC, updatedAt DESC
    """)
    fun observeBetween(start: Long, end: Long): Flow<List<Movement>>

    @Query("SELECT * FROM movements WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): Movement?

    @Insert
    suspend fun insert(m: Movement): Long

    @Update
    suspend fun updateRaw(m: Movement)

    @Query("""
        UPDATE movements SET 
            title = :title, 
            type = :type, 
            amount = :amount, 
            date = :date, 
            stripeColorHex = :stripeColorHex,
            updatedAt = :ts
        WHERE id = :id
    """)
    suspend fun setCoreFields(
        id: Long,
        title: String,
        type: String,
        amount: Long,
        date: Long,
        stripeColorHex: String,
        ts: Long = System.currentTimeMillis()
    )

    @Query("UPDATE movements SET amount = :amount, updatedAt = :ts WHERE id = :id")
    suspend fun setAmount(id: Long, amount: Long, ts: Long = System.currentTimeMillis())

    @Query("UPDATE movements SET isActive = 0, updatedAt = :ts WHERE id = :id")
    suspend fun softDelete(id: Long, ts: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM movements")
    fun observeCount(): Flow<Int>
}
