package com.example.safefitness.data

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.*

class DataResponder(private val context: Context) {

    fun sendDataToWatch(dataPath: String, jsonData: String) {
        val path = if (dataPath.startsWith("/")) dataPath else "/$dataPath"

        val putDataReq: PutDataRequest = PutDataMapRequest.create(path).apply {
            dataMap.putString("fitnessData", jsonData)
        }.asPutDataRequest()

        Wearable.getDataClient(context).putDataItem(putDataReq).addOnSuccessListener {
            Log.d("DataResponder", "Data sent successfully to path: $path")
        }.addOnFailureListener { e ->
            Log.e("DataResponder", "Failed to send data to path: $path", e)
        }
    }
}

