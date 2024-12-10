package com.example.safefitness

import android.content.Context
import android.util.Log
import com.example.safefitness.data.FitnessDatabase
import com.google.android.gms.wearable.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray

class WatchDataListener(private val context: Context) : DataClient.OnDataChangedListener {

    private val fitnessDao = FitnessDatabase.getDatabase(context).fitnessDao()

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.use { buffer ->
            buffer.forEach { event ->
                if (event.type == DataEvent.TYPE_CHANGED &&
                    event.dataItem.uri.path?.startsWith("/fitness_data_back") == true
                ) {
                    val dataMapItem = DataMapItem.fromDataItem(event.dataItem)
                    val jsonData = dataMapItem.dataMap.getString("fitnessData")

                    if (jsonData != null) {
                        Log.d("WatchDataListener", "Data received from phone: $jsonData")
                        handleReceivedData(jsonData)
                    }
                }
            }
        }
    }

    private fun handleReceivedData(jsonData: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val jsonArray = JSONArray(jsonData)

                for (i in 0 until jsonArray.length()) {
                    val jsonObject = jsonArray.getJSONObject(i)
                    val date = jsonObject.getString("date")
                    val steps = jsonObject.optInt("steps", -1).takeIf { it >= 0 }
                    val heartRate = jsonObject.optDouble("heartRate", -1.0).takeIf { it >= 0 }?.toFloat()

                    val existingData = fitnessDao.getEntryByDate(date)
                    if (existingData != null &&
                        (existingData.steps == steps || steps == null) &&
                        (existingData.heartRate == heartRate || heartRate == null)
                    ) {
                        Log.d("WatchDataListener", "Marking data as synced: $existingData")
                        fitnessDao.markDataAsSyncedWithDuplicates(listOf(existingData.id))
                    }
                }
            } catch (e: Exception) {
                Log.e("WatchDataListener", "Error handling received data: ${e.message}", e)
            }
        }
    }
}
