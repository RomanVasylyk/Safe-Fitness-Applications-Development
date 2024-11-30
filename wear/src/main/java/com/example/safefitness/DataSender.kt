package com.example.safefitness

import android.content.Context
import android.util.Log
import com.example.safefitness.data.FitnessDatabase
import com.google.android.gms.wearable.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class DataSender(context: Context) {
    private val dataClient: DataClient = Wearable.getDataClient(context)
    private val fitnessDao = FitnessDatabase.getDatabase(context).fitnessDao()

    suspend fun sendUnsyncedDataToPhone() {
        val unsyncedData = withContext(Dispatchers.IO) {
            fitnessDao.getUnsyncedData()
        }

        if (unsyncedData.isNotEmpty()) {
            val batchSize = 300
            val batches = unsyncedData.chunked(batchSize)
            batches.forEachIndexed { index, batch ->
                val jsonArray = JSONArray()
                val idsToMarkSynced = mutableListOf<Int>()

                for (data in batch) {
                    val jsonObject = JSONObject()
                    jsonObject.put("date", data.date)
                    jsonObject.put("steps", data.steps)
                    jsonObject.put("heartRate", data.heartRate)
                    jsonArray.put(jsonObject)
                    idsToMarkSynced.add(data.id)
                }

                val timestamp = System.currentTimeMillis()
                val path = "/fitness_data_$timestamp"

                val putDataReq: PutDataRequest = PutDataMapRequest.create(path).apply {
                    dataMap.putString("fitnessData", jsonArray.toString())
                }.asPutDataRequest()

                dataClient.putDataItem(putDataReq).addOnSuccessListener {
                    Log.d("DataSender", "Batch $index sent to phone successfully")
                }.addOnFailureListener { e ->
                    Log.e("DataSender", "Failed to send batch $index to phone", e)
                }
            }
        }
    }
}
