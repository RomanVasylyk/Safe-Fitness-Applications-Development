package com.example.safefitness

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable

class DataSender(context: Context) {
    private val dataClient: DataClient = Wearable.getDataClient(context)

    fun sendData(key: String, value: Float) {
        val putDataReq: PutDataRequest = PutDataMapRequest.create("/data").run {
            dataMap.putFloat(key, value)
            asPutDataRequest()
        }
        dataClient.putDataItem(putDataReq).addOnSuccessListener {
            Log.d("DataSender", "$key: $value sent successfully")
        }.addOnFailureListener {
            Log.e("DataSender", "Failed to send $key: $value", it)
        }
    }
}
