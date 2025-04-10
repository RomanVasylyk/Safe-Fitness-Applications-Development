package com.example.safefitness.data.repository

import android.util.Log
import com.example.safefitness.data.local.dao.FitnessDao
import com.example.safefitness.data.local.dao.SentBatchDao
import com.example.safefitness.data.local.entity.FitnessEntity
import com.example.safefitness.data.local.entity.SentBatchEntity
import com.example.safefitness.data.remote.WearDataSender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FitnessRepositoryImpl(
    private val fitnessDao: FitnessDao,
    private val sentBatchDao: SentBatchDao,
    private val wearDataSender: WearDataSender
) : FitnessRepository {

    override suspend fun insertOrUpdateData(entity: FitnessEntity) {
        withContext(Dispatchers.IO) {
            fitnessDao.insertOrUpdateEntry(entity)
        }
    }

    override suspend fun getStepsForCurrentDay(date: String): Int? {
        return fitnessDao.getStepsForCurrentDay(date)
    }

    override suspend fun getLastHeartRateForCurrentDay(date: String): Float? {
        return fitnessDao.getLastHeartRateForCurrentDay(date)
    }

    override suspend fun getUnsyncedData(): List<FitnessEntity> {
        return fitnessDao.getUnsyncedData()
    }

    override suspend fun deleteOldData(cutoff: String) {
        fitnessDao.deleteOldData(cutoff)
    }

    override suspend fun markDataAsSynced(ids: List<Int>) {
        fitnessDao.markDataAsSynced(ids)
    }

    override suspend fun syncDataWithPhone() {
        val connectedNodes = wearDataSender.getConnectedNodes()
        if (connectedNodes.isEmpty()) {
            Log.d("FitnessRepositoryImpl", "No connected nodes, skipping sync.")
            return
        }
        val unsynced = fitnessDao.getUnsyncedData()
        if (unsynced.isNotEmpty()) {
            val chunkSize = 300
            val chunks = unsynced.chunked(chunkSize)
            for ((index, chunk) in chunks.withIndex()) {
                val json = wearDataSender.createBatchJson(chunk)
                val timestamp = System.currentTimeMillis()
                val batchEntity = SentBatchEntity(
                    timestamp = timestamp,
                    jsonData = json,
                    isConfirmed = false
                )
                val batchId = sentBatchDao.insertSentBatch(batchEntity).toInt()
                wearDataSender.sendBatch(batchEntity.copy(id = batchId), index)
            }
            Log.d("FitnessRepositoryImpl", "Sync triggered for ${unsynced.size} records.")
        } else {
            Log.d("FitnessRepositoryImpl", "No unsynced data to send.")
        }
    }
}
