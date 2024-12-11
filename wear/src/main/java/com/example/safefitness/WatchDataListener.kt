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
    private val sentBatchDao = FitnessDatabase.getDatabase(context).sentBatchDao()
    private val fitnessDao = FitnessDatabase.getDatabase(context).fitnessDao()

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.use { buffer ->
            for (event in buffer) {
                if (event.type == DataEvent.TYPE_CHANGED &&
                    event.dataItem.uri.path?.startsWith("/fitness_data_confirmed_") == true
                ) {
                    val dataMapItem = DataMapItem.fromDataItem(event.dataItem)
                    val batchId = dataMapItem.dataMap.getInt("batchId", -1)

                    if (batchId != -1) {
                        CoroutineScope(Dispatchers.IO).launch {
                            sentBatchDao.markBatchConfirmed(batchId)
                            Log.d("WatchDataListener", "Batch $batchId confirmed by phone.")

                            val batch = sentBatchDao.getBatchById(batchId)
                            if (batch != null) {
                                val jsonArray = JSONArray(batch.jsonData)
                                val idsToMarkSynced = mutableListOf<Int>()

                                for (i in 0 until jsonArray.length()) {
                                    val jsonObject = jsonArray.getJSONObject(i)
                                    val date = jsonObject.getString("date")
                                    val steps = jsonObject.optInt("steps", -1).takeIf { it >= 0 }
                                    val heartRate = jsonObject.optDouble("heartRate", -1.0).takeIf { it >= 0 }?.toFloat()

                                    val entry = fitnessDao.getEntryByDate(date)
                                    if (entry != null &&
                                        (steps == null || entry.steps == steps) &&
                                        (heartRate == null || entry.heartRate == heartRate)
                                    ) {
                                        idsToMarkSynced.add(entry.id)
                                    }
                                }

                                if (idsToMarkSynced.isNotEmpty()) {
                                    fitnessDao.markDataAsSynced(idsToMarkSynced)
                                    Log.d("WatchDataListener", "Marked ${idsToMarkSynced.size} records as synced.")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
