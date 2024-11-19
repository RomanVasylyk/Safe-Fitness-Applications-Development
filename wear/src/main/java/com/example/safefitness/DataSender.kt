package com.example.safefitness

import android.content.Context
import android.util.Log
import com.example.safefitness.data.FitnessDatabase
import com.google.android.gms.wearable.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class DataSender(context: Context) {
    private val dataClient: DataClient = Wearable.getDataClient(context)
    private val fitnessDao = FitnessDatabase.getDatabase(context).fitnessDao()

    suspend fun sendAllDataToPhone() {
        val dataList = withContext(Dispatchers.IO) { fitnessDao.getAllData() }
        if (dataList.isNotEmpty()) {
            val jsonArray = JSONArray()
            for (data in dataList) {
                val jsonObject = JSONObject()
                jsonObject.put("date", data.date)
                jsonObject.put("steps", data.steps)
                jsonObject.put("heartRate", data.heartRate)
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
