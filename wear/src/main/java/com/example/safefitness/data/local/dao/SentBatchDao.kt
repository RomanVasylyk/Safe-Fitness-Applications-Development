package com.example.safefitness.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.safefitness.data.local.entity.SentBatchEntity

@Dao
interface SentBatchDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSentBatch(batch: SentBatchEntity): Long

    @Query("UPDATE sent_batches SET isConfirmed = 1 WHERE id = :id")
    suspend fun markBatchConfirmed(id: Int)

    @Query("SELECT * FROM sent_batches WHERE id = :id LIMIT 1")
    suspend fun getBatchById(id: Int): SentBatchEntity?

    @Query("DELETE FROM sent_batches WHERE isConfirmed = 1 AND timestamp < :olderThan")
    suspend fun deleteOldConfirmedBatches(olderThan: Long)

    @Query("SELECT * FROM sent_batches WHERE isConfirmed = 0 ORDER BY timestamp ASC")
    suspend fun getUnconfirmedBatches(): List<SentBatchEntity>
}
