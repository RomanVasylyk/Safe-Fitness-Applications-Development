package com.example.safefitness.data

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class WearDataListener(
    private val dataHandler: DataHandler,
    private val onDataUpdated: () -> Unit,
    private val context: Context
) : DataClient.OnDataChangedListener {

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        val eventsToProcess = mutableListOf<Pair<String, String>>()

        dataEvents.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED &&
                event.dataItem.uri.path?.startsWith("/data_batch_") == true
            ) {
                val dataPath = event.dataItem.uri.path ?: return@forEach
                val dataMapItem = DataMapItem.fromDataItem(event.dataItem)
                val jsonData = dataMapItem.dataMap.getString("fitnessData") ?: return@forEach

                eventsToProcess.add(Pair(dataPath, jsonData))
            }
        }

        eventsToProcess.forEach { (dataPath, jsonData) ->
            CoroutineScope(Dispatchers.IO).launch {
                dataHandler.saveData(jsonData)
                sendConfirmationToWatch(dataPath)
                onDataUpdated()
            }
        }
    }

    private suspend fun sendConfirmationToWatch(dataPath: String) {
        val nodeClient = Wearable.getNodeClient(context)
        val messageClient = Wearable.getMessageClient(context)

        val nodes: List<Node> = nodeClient.connectedNodes.await()

        nodes.forEach { node: Node ->
            messageClient.sendMessage(
                node.id,
                "/confirmation",
                dataPath.toByteArray()
            ).addOnSuccessListener {
                Log.d("WearDataListener", "Confirmation sent to ${node.displayName}")
            }.addOnFailureListener { e ->
                Log.e("WearDataListener", "Failed to send confirmation to ${node.displayName}", e)
            }
        }
    }
}
