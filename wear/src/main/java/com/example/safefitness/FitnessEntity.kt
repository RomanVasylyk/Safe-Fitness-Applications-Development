package com.example.safefitness

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fitness_data")
data class FitnessEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val date: String,
    val steps: Int?,
    val heartRate: Float?
)
