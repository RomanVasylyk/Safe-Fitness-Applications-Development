package com.example.safefitness.data.remote

import android.content.Context
import android.util.Log
import com.example.safefitness.data.local.entity.SentBatchEntity
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class WearDataSender(context: Context) {
    private val dataClient: DataClient = Wearable.getDataClient(context)
    private val nodeClient: NodeClient = Wearable.getNodeClient(context)

    suspend fun getConnectedNodes(): List<Node> {
        return withContext(Dispatchers.IO) {
            try {
                Tasks.await(nodeClient.connectedNodes, 5, TimeUnit.SECONDS)
            } catch (e: Exception) {
                Log.e("WearDataSender", "Error checking connected nodes: ${e.message}", e)
                emptyList()
            }
        }
    }

    fun createBatchJson(entities: List<com.example.safefitness.data.local.entity.FitnessEntity>): String {
        val jsonArray = JSONArray()
        entities.forEach { data ->
            val obj = JSONObject()
            obj.put("entryId", data.id)
            obj.put("date", data.date)
            obj.put("steps", data.steps)
            obj.put("heartRate", data.heartRate)
            jsonArray.put(obj)
        }
        return jsonArray.toString()
    }

    fun sendBatch(batch: SentBatchEntity, chunkIndex: Int) {
        val path = "/fitness_data_batch_${batch.id}_$chunkIndex"
        val request = PutDataMapRequest.create(path).apply {
            dataMap.putString("fitnessData", batch.jsonData)
            dataMap.putInt("batchId", batch.id)
            dataMap.putLong("timestamp", System.currentTimeMillis())
        }.asPutDataRequest()

        dataClient.putDataItem(request)
            .addOnSuccessListener {
                Log.d("WearDataSender", "Batch ${batch.id}, chunk $chunkIndex sent")
            }
            .addOnFailureListener { e ->
                Log.e("WearDataSender", "Failed to send batch ${batch.id}, chunk $chunkIndex", e)
            }
    }
}
