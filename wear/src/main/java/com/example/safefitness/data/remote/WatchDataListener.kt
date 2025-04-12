package com.example.safefitness.data.remote

import android.content.Context
import android.util.Log
import com.example.safefitness.data.local.db.FitnessDatabase
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray

class WatchDataListener(private val context: Context) : DataClient.OnDataChangedListener {

    private val db = FitnessDatabase.getInstance(context)
    private val sentBatchDao = db.sentBatchDao()
    private val fitnessDao = db.fitnessDao()
    private val repository = com.example.safefitness.data.repository.FitnessRepositoryImpl(
        fitnessDao,
        sentBatchDao,
        WearDataSender(context)
    )

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
                                val arr = JSONArray(batch.jsonData)
                                val list = mutableListOf<Int>()
                                for (i in 0 until arr.length()) {
                                    val o = arr.getJSONObject(i)
                                    val e = o.optInt("entryId", -1)
                                    if (e != -1) list.add(e)
                                }
                                repository.onPhoneAcknowledgementReceived(batchId, list)
                            }
                        }
                    }
                }
            }
        }
    }
}
