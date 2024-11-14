package com.example.safefitness

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable

class MainActivity : AppCompatActivity(), DataClient.OnDataChangedListener {

    private var lastHeartRate: Float = 0f
    private var lastSteps: Float = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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

                    Toast.makeText(this@MainActivity, "Data received", Toast.LENGTH_SHORT).show()
                }
            }
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
