package com.example.finazas.data.local.dao

// package com.fintide.app.data
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.finazas.data.local.entity.User

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveUser(): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: User): Long

    @Update
    suspend fun update(user: User)

    @Query("UPDATE users SET isActive = 0")
    suspend fun deactivateAll()
}
