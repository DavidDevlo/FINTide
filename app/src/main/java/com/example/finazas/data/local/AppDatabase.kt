// com/example/ventas/data/local/AppDatabase.kt
package com.example.finazas.data.local



import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.finazas.data.local.dao.GoalDao
import com.example.finazas.data.local.dao.MovementDao
import com.example.finazas.data.local.dao.PaymentCardDao
import com.example.finazas.data.local.dao.SubscriptionDao
import com.example.finazas.data.local.entity.Goal
import com.example.finazas.data.local.entity.Movement
import com.example.finazas.data.local.entity.PaymentCard
import com.example.finazas.data.local.entity.Subscription
import com.example.finazas.data.repo.PaymentCardRepository

@Database(entities = [Goal::class,
    Movement::class,
    Subscription::class,
    PaymentCard::class
                     ], version = 3)
abstract class AppDatabase : RoomDatabase() {

    abstract fun goalDao(): GoalDao
    abstract fun movementDao(): MovementDao
    abstract fun subscriptionDao(): SubscriptionDao

    abstract fun paymentCardDao(): PaymentCardDao


    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context, AppDatabase::class.java, "homefinanzas.db")
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
