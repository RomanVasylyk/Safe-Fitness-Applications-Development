package com.example.safefitness.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.safefitness.data.local.dao.FitnessDao
import com.example.safefitness.data.local.dao.SentBatchDao
import com.example.safefitness.data.local.entity.FitnessEntity
import com.example.safefitness.data.local.entity.SentBatchEntity

@Database(
    entities = [FitnessEntity::class, SentBatchEntity::class],
    version = 8,
    exportSchema = false
)
abstract class FitnessDatabase : RoomDatabase() {
    abstract fun fitnessDao(): FitnessDao
    abstract fun sentBatchDao(): SentBatchDao

    companion object {
        @Volatile
        private var INSTANCE: FitnessDatabase? = null

        fun getInstance(context: Context): FitnessDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FitnessDatabase::class.java,
                    "fitness_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
