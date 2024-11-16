package com.example.safefitness

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import org.json.JSONArray

class MainActivity : AppCompatActivity(), DataClient.OnDataChangedListener {

    private var lastHeartRate: Float = 0f
    private var lastSteps: Float = 0f

    private lateinit var phoneDatabaseHelper: PhoneDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        phoneDatabaseHelper = PhoneDatabaseHelper(this)
        Wearable.getDataClient(this).addListener(this)
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == "/data") {
                DataMapItem.fromDataItem(event.dataItem).dataMap.apply {
                    if (containsKey("heartRate")) {
                        lastHeartRate = getFloat("heartRate", 0f)
                    }
                    if (containsKey("steps")) {
                        lastSteps = getFloat("steps", 0f)
                    }

                    findViewById<TextView>(R.id.heartText).text = "${lastHeartRate.toInt()} bpm"
                    findViewById<TextView>(R.id.stepsText).text = "${lastSteps.toInt()} steps"

                    if (containsKey("fitnessData")) {
                        val jsonData = getString("fitnessData")
                        if (jsonData != null) {
                            saveDataToDatabase(jsonData)
                        }
                    }

                    Toast.makeText(this@MainActivity, "Data received", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveDataToDatabase(jsonData: String) {
        try {
            val jsonArray = JSONArray(jsonData)
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val date = jsonObject.getString(PhoneDatabaseHelper.COLUMN_DATE)
                val steps = jsonObject.getInt(PhoneDatabaseHelper.COLUMN_STEPS)
                val heartRate = if (jsonObject.isNull(PhoneDatabaseHelper.COLUMN_HEART_RATE)) null else jsonObject.getDouble(PhoneDatabaseHelper.COLUMN_HEART_RATE).toFloat()
                phoneDatabaseHelper.insertData(date, steps, heartRate)
            }
            Toast.makeText(this, "All data saved to database", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error saving data to database", e)
        }
    }

    override fun onStart() {
        super.onStart()
        Wearable.getDataClient(this).addListener(this)
    }

    override fun onPause() {
        super.onPause()
        Wearable.getDataClient(this).removeListener(this)
    }
}
