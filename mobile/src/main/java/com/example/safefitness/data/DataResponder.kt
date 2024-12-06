package com.example.safefitness.data

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue

class DataResponder(private val context: Context) {

    private val dataQueue = LinkedBlockingQueue<Pair<String, JSONObject>>()
    private val lastSentData = ConcurrentHashMap<String, JSONObject>()
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    init {
        coroutineScope.launch {
            processQueue()
        }
    }

    fun sendDataToWatch(dataPath: String, jsonData: String) {
        val path = if (dataPath.startsWith("/")) dataPath else "/$dataPath"

        try {
            val isArray = jsonData.trim().startsWith("[")

            val putDataReq: PutDataRequest = PutDataMapRequest.create(path).apply {
                if (isArray) {
                    dataMap.putString("fitnessData", jsonData)
                } else {
                    throw IllegalArgumentException("Expected JSONArray but got a different format.")
                }
            }.asPutDataRequest()
            Log.d("DataResponder", "We are preparing data for sending: $jsonData")
            Wearable.getDataClient(context).putDataItem(putDataReq).addOnSuccessListener {
                Log.d("DataResponder", "Data sent successfully to path: $path")
            }.addOnFailureListener { e ->
                Log.e("DataResponder", "Failed to send data to path: $path", e)
            }
        } catch (e: Exception) {
            Log.e("DataResponder", "Error processing data: ${e.message}", e)
        }
    }

    private suspend fun processQueue() {
        while (true) {
            val (dataPath, jsonObject) = dataQueue.poll() ?: continue

            try {
                val putDataReq: PutDataRequest = PutDataMapRequest.create(dataPath).apply {
                    dataMap.putString("fitnessData", jsonObject.toString())
                }.asPutDataRequest()

                Wearable.getDataClient(context).putDataItem(putDataReq).addOnSuccessListener {
                    Log.d("DataResponder", "Data sent successfully for path: $dataPath")
                    lastSentData[dataPath] = jsonObject
                }.addOnFailureListener { e ->
                    Log.e("DataResponder", "Error sending data for path: $dataPath", e)
                }
            } catch (e: Exception) {
                Log.e("DataResponder", "Error sending data: ${e.message}", e)
            }

            delay(15000)
        }
    }

    private fun shouldEnqueueData(dataPath: String, newData: JSONObject): Boolean {
        val lastData = lastSentData[dataPath]
        return lastData == null || lastData.toString() != newData.toString()
    }
}



