package com.example.safefitness

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [FitnessEntity::class], version = 2, exportSchema = false)
abstract class FitnessDatabase : RoomDatabase() {
    abstract fun fitnessDao(): FitnessDao

    companion object {
        @Volatile
        private var INSTANCE: FitnessDatabase? = null

        fun getDatabase(context: Context): FitnessDatabase {
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
