package com.example.safefitness.data

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.*

class DataResponder(private val context: Context) {

    fun sendDataToWatch(dataPath: String, jsonData: String) {
        val putDataReq: PutDataRequest = PutDataMapRequest.create(dataPath).apply {
            dataMap.putString("fitnessData", jsonData)
        }.asPutDataRequest()

        Wearable.getDataClient(context).putDataItem(putDataReq).addOnSuccessListener {
            Log.d("DataResponder", "Data sent back to watch successfully")
        }.addOnFailureListener { e ->
            Log.e("DataResponder", "Failed to send data back to watch", e)
        }
    }
}
