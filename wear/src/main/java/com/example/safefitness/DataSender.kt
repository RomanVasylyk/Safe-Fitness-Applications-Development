package com.example.safefitness

import android.content.Context
import android.util.Log
import com.example.safefitness.data.FitnessDatabase
import com.google.android.gms.wearable.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class DataSender(context: Context) {
    private val dataClient: DataClient = Wearable.getDataClient(context)
    private val fitnessDao = FitnessDatabase.getDatabase(context).fitnessDao()

    suspend fun sendAllDataToPhone() {
        val dataList = withContext(Dispatchers.IO) { fitnessDao.getUnsyncedData() }
        if (dataList.isNotEmpty()) {
            val batchSize = 50
            val batches = dataList.chunked(batchSize)
            batches.forEachIndexed { index, batch ->
                val jsonArray = JSONArray()
                val batchNumber = index
                for (data in batch) {
                    val jsonObject = JSONObject()
                    jsonObject.put("date", data.date)
                    jsonObject.put("steps", data.steps)
                    jsonObject.put("heartRate", data.heartRate)
                    jsonArray.put(jsonObject)
                }

                val path = "/data_batch_$batchNumber"
                val putDataReq: PutDataRequest = PutDataMapRequest.create(path).apply {
                    dataMap.putString("fitnessData", jsonArray.toString())
                }.asPutDataRequest()

                dataClient.putDataItem(putDataReq).addOnSuccessListener {
                    Log.d("DataSender", "Batch $batchNumber sent to phone successfully")
                    CoroutineScope(Dispatchers.IO).launch {
                        batch.forEach { data ->
                            fitnessDao.updateBatchNumber(data.id, batchNumber)
                        }
                    }
                }.addOnFailureListener { e ->
                    Log.e("DataSender", "Failed to send batch $batchNumber to phone", e)
                }
            }
        }
    }

}
