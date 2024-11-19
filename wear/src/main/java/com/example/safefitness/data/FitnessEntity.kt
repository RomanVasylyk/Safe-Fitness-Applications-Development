package com.example.safefitness.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "fitness_data",
    indices = [androidx.room.Index(value = ["date", "steps", "heartRate"], unique = true)]
)
data class FitnessEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val date: String,
    val steps: Int?,
    val heartRate: Float?
)
