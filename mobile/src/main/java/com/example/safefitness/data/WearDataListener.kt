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

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.use { buffer ->
            for (event in buffer) {
                if (event.type == DataEvent.TYPE_CHANGED &&
                    event.dataItem.uri.path?.startsWith("/fitness_data_batch_") == true
                ) {
                    val dataMapItem = DataMapItem.fromDataItem(event.dataItem)
                    val jsonData = dataMapItem.dataMap.getString("fitnessData")
                    val batchId = dataMapItem.dataMap.getInt("batchId", -1)

                    if (jsonData != null && batchId != -1) {
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                dataHandler.saveData(jsonData)
                                confirmBatchReceived(batchId)
                                onDataUpdated()
                            } catch (e: Exception) {
                                Log.e("WearDataListener", "Error saving data: ${e.message}", e)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun confirmBatchReceived(batchId: Int) {
        val path = "/fitness_data_confirmed_$batchId"
        val putDataReq = PutDataMapRequest.create(path).apply {
            dataMap.putInt("batchId", batchId)
        }.asPutDataRequest()

        Wearable.getDataClient(context).putDataItem(putDataReq)
            .addOnSuccessListener {
                Log.d("WearDataListener", "Confirmed batch $batchId to watch")
            }
            .addOnFailureListener { e ->
                Log.e("WearDataListener", "Failed to confirm batch $batchId", e)
            }
    }
}
