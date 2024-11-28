package com.example.safefitness

import com.example.safefitness.data.FitnessDao
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ConfirmationListener(
    private val fitnessDao: FitnessDao
) : MessageClient.OnMessageReceivedListener {

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == "/confirmation") {
            val dataPath = String(messageEvent.data)
            CoroutineScope(Dispatchers.IO).launch {
                markDataAsSynced(dataPath)
            }
        }
    }

    private suspend fun markDataAsSynced(dataPath: String) {
        val batchNumber = dataPath.substringAfterLast("_").toIntOrNull()
        if (batchNumber != null) {
            val dataList = fitnessDao.getDataByBatchNumber(batchNumber)
            if (dataList.isNotEmpty()) {
                val ids = dataList.map { it.id }
                fitnessDao.markDataAsSynced(ids)
            }
        }
    }
}
