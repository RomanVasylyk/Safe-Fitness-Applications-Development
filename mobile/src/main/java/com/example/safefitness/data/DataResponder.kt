package com.example.safefitness.data

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue

class DataResponder(private val context: Context) {

    private val dataQueue = LinkedBlockingQueue<Pair<String, JSONObject>>()
    private val lastSentData = ConcurrentHashMap<String, JSONObject>()
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val mutex = Mutex()

    init {
        coroutineScope.launch {
            processQueue()
        }
    }

    fun sendDataToWatch(dataPath: String, jsonData: String) {
        val path = if (dataPath.startsWith("/")) dataPath else "/$dataPath"

        try {
            val isArray = jsonData.trim().startsWith("[")
            if (!isArray) throw IllegalArgumentException("Expected JSONArray but got a different format.")

            val jsonArray = JSONArray(jsonData)
            if (jsonArray.length() > 300) {
                splitAndEnqueueData(path, jsonArray)
            } else {
                enqueueSinglePacket(path, jsonArray)
            }
        } catch (e: Exception) {
            Log.e("DataResponder", "Error processing data: ${e.message}", e)
        }
    }

    private fun enqueueSinglePacket(path: String, jsonArray: JSONArray) {
        val jsonObject = JSONObject().apply {
            put("fitnessData", jsonArray)
        }

        if (shouldEnqueueData(path, jsonObject)) {
            dataQueue.offer(Pair(path, jsonObject))
        } else {
            Log.d("DataResponder", "Data not enqueued because it's a duplicate for path: $path")
        }
    }

    private fun splitAndEnqueueData(basePath: String, jsonArray: JSONArray) {
        val totalParts = (jsonArray.length() + 299) / 300
        var partIndex = 0

        for (i in 0 until jsonArray.length() step 300) {
            val chunk = JSONArray()
            for (j in i until minOf(i + 300, jsonArray.length())) {
                chunk.put(jsonArray[j])
            }

            val chunkPath = "$basePath/part_$partIndex"
            val jsonObject = JSONObject().apply {
                put("fitnessData", chunk)
                put("totalParts", totalParts)
                put("partIndex", partIndex)
            }

            if (shouldEnqueueData(chunkPath, jsonObject)) {
                dataQueue.offer(Pair(chunkPath, jsonObject))
            } else {
                Log.d("DataResponder", "Chunk not enqueued because it's a duplicate for path: $chunkPath")
            }

            partIndex++
        }
    }

    private suspend fun processQueue() {
        while (true) {
            val (dataPath, jsonObject) = dataQueue.take()

            try {
                val putDataReq: PutDataRequest = PutDataMapRequest.create(dataPath).apply {
                    dataMap.putString("fitnessData", jsonObject.optJSONArray("fitnessData")?.toString() ?: "[]")
                    if (jsonObject.has("totalParts")) {
                        dataMap.putString("metadata", JSONObject().apply {
                            put("totalParts", jsonObject.getInt("totalParts"))
                            put("partIndex", jsonObject.getInt("partIndex"))
                        }.toString())
                    }
                }.asPutDataRequest()

                val task = Wearable.getDataClient(context).putDataItem(putDataReq)
                val result = task.await()

                Log.d("DataResponder", "Data sent successfully for path: $dataPath")
                lastSentData[dataPath] = jsonObject
            } catch (e: Exception) {
                Log.e("DataResponder", "Error sending data for path: $dataPath, ${e.message}", e)
            }

            delay(15000)
        }
    }

    private fun shouldEnqueueData(dataPath: String, newData: JSONObject): Boolean {
        val lastData = lastSentData[dataPath]
        return lastData == null || lastData.toString() != newData.toString()
    }
}

