package com.example.safefitness.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sent_batches")
data class SentBatchEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val jsonData: String,
    val isConfirmed: Boolean = false
)

