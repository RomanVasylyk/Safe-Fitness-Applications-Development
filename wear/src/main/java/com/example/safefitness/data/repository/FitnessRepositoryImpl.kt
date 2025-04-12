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

    @Volatile
    private var phoneResponding = false
    private var lastSendTimeMillis = 0L
    private val lock = Any()
    private val chunkSize = 300

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

    override suspend fun syncDataWithPhone() = withContext(Dispatchers.IO) {
        val nodes = wearDataSender.getConnectedNodes()
        if (nodes.isEmpty()) {
            Log.d("FitnessRepositoryImpl", "No connected nodes, skipping sync.")
            return@withContext
        }
        val unconfirmed = sentBatchDao.getUnconfirmedBatches()
        if (unconfirmed.isNotEmpty()) {
            unconfirmed.forEach {
                wearDataSender.sendBatch(it, 0)
            }
            return@withContext
        }
        val now = System.currentTimeMillis()
        synchronized(lock) {
            if (!phoneResponding && now - lastSendTimeMillis < 5000) {
                Log.d("FitnessRepositoryImpl", "Phone not responding, skip new batch (cooldown)")
                return@withContext
            }
        }
        val unsynced = fitnessDao.getUnsyncedData()
        if (unsynced.isEmpty()) {
            Log.d("FitnessRepositoryImpl", "No unsynced data to send.")
            return@withContext
        }
        val chunks = unsynced.chunked(chunkSize)
        for ((index, chunk) in chunks.withIndex()) {
            val json = wearDataSender.createBatchJson(chunk)
            val timestamp = System.currentTimeMillis()
            val batch = SentBatchEntity(timestamp = timestamp, jsonData = json, isConfirmed = false)
            val batchId = sentBatchDao.insertSentBatch(batch).toInt()
            wearDataSender.sendBatch(batch.copy(id = batchId), index)
        }
        synchronized(lock) {
            lastSendTimeMillis = now
        }
        Log.d("FitnessRepositoryImpl", "Sync triggered for ${unsynced.size} records.")
    }

    suspend fun onPhoneAcknowledgementReceived(batchId: Int, ids: List<Int>) {
        sentBatchDao.markBatchConfirmed(batchId)
        if (ids.isNotEmpty()) fitnessDao.markDataAsSynced(ids)
        phoneResponding = true
        val left = fitnessDao.getUnsyncedData()
        if (left.isEmpty()) phoneResponding = false
        Log.d("FitnessRepositoryImpl", "Batch $batchId confirmed, phone responding = $phoneResponding")
    }
}
