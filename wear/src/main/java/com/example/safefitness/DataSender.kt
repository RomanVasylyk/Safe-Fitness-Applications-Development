package com.example.safefitness

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.*
import org.json.JSONArray
import org.json.JSONObject

class DataSender(context: Context) {
    private val dataClient: DataClient = Wearable.getDataClient(context)

    fun sendAllDataToPhone(databaseHelper: FitnessDatabaseHelper) {
        val dataList = databaseHelper.getAllData()
        if (dataList.isNotEmpty()) {
            val jsonArray = JSONArray()
            for (data in dataList) {
                val jsonObject = JSONObject()
                jsonObject.put(FitnessDatabaseHelper.COLUMN_DATE, data[FitnessDatabaseHelper.COLUMN_DATE])
                jsonObject.put(FitnessDatabaseHelper.COLUMN_STEPS, data[FitnessDatabaseHelper.COLUMN_STEPS])
                jsonObject.put(FitnessDatabaseHelper.COLUMN_HEART_RATE, data[FitnessDatabaseHelper.COLUMN_HEART_RATE])
                jsonArray.put(jsonObject)
            }

            val putDataReq: PutDataRequest = PutDataMapRequest.create("/data").apply {
                dataMap.putString("fitnessData", jsonArray.toString())
            }.asPutDataRequest()

            dataClient.putDataItem(putDataReq).addOnSuccessListener {
                Log.d("DataSender", "All data sent to phone successfully")
            }.addOnFailureListener { e ->
                Log.e("DataSender", "Failed to send data to phone", e)
            }
        }
    }
}
