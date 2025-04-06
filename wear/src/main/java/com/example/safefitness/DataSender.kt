package com.example.safefitness

import android.content.Context
import android.util.Log
import com.example.safefitness.data.FitnessDatabase
import com.example.safefitness.data.SentBatchEntity
import com.google.android.gms.wearable.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.google.android.gms.tasks.Tasks
import java.util.concurrent.TimeUnit

class DataSender(context: Context) {
    private val dataClient: DataClient = Wearable.getDataClient(context)
    private val nodeClient: NodeClient = Wearable.getNodeClient(context)
    private val fitnessDao = FitnessDatabase.getDatabase(context).fitnessDao()
    private val sentBatchDao = FitnessDatabase.getDatabase(context).sentBatchDao()

    private var lastSendTime: Long = 0
    private val sendMutex = Mutex()

    suspend fun sendUnsyncedDataToPhone() {
        sendMutex.withLock {
            val currentTime = System.currentTimeMillis()

            if (currentTime - lastSendTime < 10000) {
                Log.d("DataSender", "Skipping data send: waiting 10 seconds")
                return
            }

            val connectedNodes = withContext(Dispatchers.IO) {
                getConnectedNodes()
            }
            if (connectedNodes.isEmpty()) {
                Log.d("DataSender", "No connected nodes. Skipping data send.")
                return
            }

            val unconfirmedBatches = withContext(Dispatchers.IO) {
                sentBatchDao.getUnconfirmedBatches()
            }

            if (unconfirmedBatches.isNotEmpty()) {
                val firstUnconfirmed = unconfirmedBatches.first()
                Log.d("DataSender", "Resending first unconfirmed batch (ID: ${firstUnconfirmed.id}). Others will wait until confirmation.")
                sendBatchToPhone(firstUnconfirmed)

                lastSendTime = System.currentTimeMillis()
                return
            }

            val unsyncedData = withContext(Dispatchers.IO) {
                fitnessDao.getUnsyncedData()
            }

            if (unsyncedData.isNotEmpty()) {
                val batchSize = 300
                val chunks = unsyncedData.chunked(batchSize)
                for (chunk in chunks) {
                    val jsonArray = JSONArray()
                    for (data in chunk) {
                        val jsonObject = JSONObject()
                        jsonObject.put("entryId", data.id)
                        jsonObject.put("date", data.date)
                        jsonObject.put("steps", data.steps)
                        jsonObject.put("heartRate", data.heartRate)
                        jsonArray.put(jsonObject)
                    }
                    val timestamp = System.currentTimeMillis()
                    val batchEntity = SentBatchEntity(
                        timestamp = timestamp,
                        jsonData = jsonArray.toString(),
                        isConfirmed = false
                    )
                    val batchId = withContext(Dispatchers.IO) {
                        sentBatchDao.insertSentBatch(batchEntity).toInt()
                    }
                    sendBatchToPhone(batchEntity.copy(id = batchId))
                }
                lastSendTime = System.currentTimeMillis()
            } else {
                Log.d("DataSender", "No unsynced data to send")
                lastSendTime = System.currentTimeMillis()
            }
        }
    }

    private fun sendBatchToPhone(batch: SentBatchEntity) {
        val path = "/fitness_data_batch_${batch.id}"
        val putDataReq = PutDataMapRequest.create(path).apply {
            dataMap.putString("fitnessData", batch.jsonData)
            dataMap.putInt("batchId", batch.id)
            dataMap.putLong("timestamp", System.currentTimeMillis())
        }.asPutDataRequest()
        dataClient.putDataItem(putDataReq)
            .addOnSuccessListener {
                Log.d("DataSender", "Batch ${batch.id} sent to phone")
            }
            .addOnFailureListener { e ->
                Log.e("DataSender", "Failed to send batch ${batch.id}", e)
            }
    }

    private suspend fun getConnectedNodes(): List<Node> {
        return withContext(Dispatchers.IO) {
            try {
                Tasks.await(nodeClient.connectedNodes, 5, TimeUnit.SECONDS)
            } catch (e: Exception) {
                Log.e("DataSender", "Error checking connected nodes: ${e.message}", e)
                emptyList<Node>()
            }
        }
    }
}