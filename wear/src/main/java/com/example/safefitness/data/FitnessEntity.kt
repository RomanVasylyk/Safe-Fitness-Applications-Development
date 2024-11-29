package com.example.safefitness.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "fitness_data",
    indices = [Index(value = ["date", "steps", "heartRate"], unique = true)]
)
data class FitnessEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val date: String,
    val steps: Int?,
    val heartRate: Float?,
    val isSynced: Boolean = false,//delete
    val batchNumber: Int? = null//delete
)

