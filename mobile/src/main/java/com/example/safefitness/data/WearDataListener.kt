package com.example.safefitness.data

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WearDataListener(
    private val dataHandler: DataHandler,
    private val onDataUpdated: () -> Unit,
    private val context: Context
) : DataClient.OnDataChangedListener {

    private val dataResponder = DataResponder(context)

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        val eventsToProcess = mutableListOf<Pair<String, String>>()

        dataEvents.use { buffer ->
            buffer.forEach { event ->
                try {
                    if (event.type == DataEvent.TYPE_CHANGED &&
                        event.dataItem.uri.path?.startsWith("/fitness_data_") == true
                    ) {
                        val dataPath = event.dataItem.uri.path ?: return@forEach
                        val dataMapItem = DataMapItem.fromDataItem(event.dataItem)
                        val jsonData = dataMapItem.dataMap.getString("fitnessData") ?: return@forEach

                        eventsToProcess.add(Pair(dataPath, jsonData))
                    }
                } catch (e: Exception) {
                    Log.e("WearDataListener", "Error processing DataEvent: ${e.message}", e)
                }
            }
        }

        eventsToProcess.forEach { (_, jsonData) ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    dataHandler.saveData(jsonData)
                    dataResponder.sendDataToWatch("fitness_data_back", jsonData) 
                    onDataUpdated()
                } catch (e: Exception) {
                    Log.e("WearDataListener", "Error saving data or responding to watch: ${e.message}", e)
                }
            }
        }
    }
}
