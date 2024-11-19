package com.example.safefitness.data

import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WearDataListener(private val dataHandler: DataHandler, private val onDataUpdated: () -> Unit) : DataClient.OnDataChangedListener {

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == "/data") {
                DataMapItem.fromDataItem(event.dataItem).dataMap.getString("fitnessData")?.let { jsonData ->
                    CoroutineScope(Dispatchers.IO).launch {
                        dataHandler.saveData(jsonData)
                        onDataUpdated()
                    }
                }
            }
        }
    }
}
