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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import lecho.lib.hellocharts.gesture.ContainerScrollType
import lecho.lib.hellocharts.gesture.ZoomType
import lecho.lib.hellocharts.model.*
import lecho.lib.hellocharts.view.LineChartView
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(), DataClient.OnDataChangedListener {

    private lateinit var stepsGraph: LineChartView
    private lateinit var heartRateGraph: LineChartView
    private lateinit var totalStepsText: TextView
    private lateinit var lastHeartRateText: TextView
    private lateinit var fitnessDao: FitnessDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        stepsGraph = findViewById(R.id.stepsGraph)
        heartRateGraph = findViewById(R.id.heartRateGraph)
        totalStepsText = findViewById(R.id.totalStepsText)
        lastHeartRateText = findViewById(R.id.lastHeartRateText)

        val database = FitnessDatabase.getDatabase(this)
        fitnessDao = database.fitnessDao()

        Wearable.getDataClient(this).addListener(this)

        updateData()

//        clearDatabase()
//        Toast.makeText(this, "Database cleared successfully", Toast.LENGTH_SHORT).show()
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == "/data") {
                DataMapItem.fromDataItem(event.dataItem).dataMap.apply {
                    if (containsKey("fitnessData")) {
                        val jsonData = getString("fitnessData")
                        Log.d("DataReceiver", "Received data: $jsonData")

                        if (jsonData != null) {
                            saveDataToDatabase(jsonData)
                            updateData()

                            val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                            Toast.makeText(this@MainActivity, "Data received at $currentTime", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun saveDataToDatabase(jsonData: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val jsonArray = JSONArray(jsonData)
                for (i in 0 until jsonArray.length()) {
                    val jsonObject = jsonArray.getJSONObject(i)
                    val date = jsonObject.getString("date")
                    val steps = if (jsonObject.has("steps")) jsonObject.getInt("steps") else null
                    val heartRate = if (jsonObject.has("heartRate")) jsonObject.getDouble("heartRate").toFloat() else null

                    Log.d("DatabaseSave", "Parsed data: date=$date, steps=$steps, heartRate=$heartRate")
                    updateData()
                    val exists = fitnessDao.dataExists(date, steps, heartRate)
                    if (exists == 0) {
                        fitnessDao.insertData(FitnessEntity(date = date, steps = steps, heartRate = heartRate))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun updateData() {
        CoroutineScope(Dispatchers.IO).launch {
            val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            val stepsList = fitnessDao.getDataForCurrentDay(currentDate)
                .mapNotNull { it.steps?.let { steps -> it.date to steps as Number } }

            val heartRateList = fitnessDao.getDataForCurrentDay(currentDate)
                .mapNotNull { it.heartRate?.let { heartRate -> it.date to heartRate as Number } }

            val aggregatedSteps = aggregateDataByHour(stepsList)
            val aggregatedHeartRate = aggregateHeartRateBy5Minutes(heartRateList)

            val totalSteps = fitnessDao.getTotalStepsForCurrentDay(currentDate)
            val lastHeartRate = fitnessDao.getLastHeartRateForCurrentDay(currentDate)

            runOnUiThread {
                totalStepsText.text = "Total Steps: $totalSteps"
                lastHeartRateText.text = "Last Heart Rate: ${lastHeartRate ?: "N/A"} bpm"

                updateGraph(stepsGraph, aggregatedSteps, "Steps", "Time", "Steps")
                updateGraph(heartRateGraph, aggregatedHeartRate, "Heart Rate", "Time", "BPM")
            }
        }
    }

    private fun updateGraph(
        graph: LineChartView,
        data: List<Pair<String, Number>>,
        title: String,
        xAxisName: String,
        yAxisName: String
    ) {
        if (data.isEmpty()) return

        val timeLabels = data.mapIndexed { index, pair ->
            val time = pair.first.split(" ")[1]
            AxisValue(index.toFloat()).setLabel(time)
        }

        val points = data.mapIndexed { index, pair ->
            PointValue(index.toFloat(), pair.second.toFloat())
        }

        val line = Line(points).apply {
            color = resources.getColor(R.color.purple_200, null)
            isCubic = true
            setHasLabels(true)
            setHasLines(true)
            setHasPoints(true)
            pointRadius = 4
        }

        val chartData = LineChartData(listOf(line)).apply {
            axisXBottom = Axis(timeLabels).apply {
                name = xAxisName
                textColor = resources.getColor(R.color.black, null)
                textSize = 12
                setHasLines(true)
            }
            axisYLeft = Axis().apply {
                name = yAxisName
                textColor = resources.getColor(R.color.black, null)
                textSize = 12
                setHasLines(true)
            }
        }

        graph.lineChartData = chartData

        val minY = points.minOf { it.y }
        val maxY = points.maxOf { it.y }

        val viewport = Viewport().apply {
            top = maxY + (maxY - minY) * 0.1f
            bottom = minY - (maxY - minY) * 0.1f
            left = -0.5f
            right = points.size.toFloat() - 1 + 1f
        }

        graph.apply {
            maximumViewport = viewport
            currentViewport = viewport
            isInteractive = true
            zoomType = ZoomType.HORIZONTAL_AND_VERTICAL
            setContainerScrollEnabled(true, ContainerScrollType.HORIZONTAL)
        }
    }



    private fun aggregateDataByHour(data: List<Pair<String, Number>>): List<Pair<String, Number>> {
        val aggregatedData = mutableMapOf<String, MutableList<Number>>()

        data.forEach { (time, value) ->
            val hour = time.split(":")[0]
            aggregatedData.getOrPut(hour) { mutableListOf() }.add(value)
        }

        return aggregatedData.map { (hour, values) ->
            val label = "$hour:30"
            val aggregatedValue = if (values.first() is Int) {
                values.sumBy { it.toInt() }
            } else {
                values.map { it.toFloat() }.average().toFloat()
            }
            label to aggregatedValue
        }
    }
    private fun aggregateHeartRateBy5Minutes(data: List<Pair<String, Number>>): List<Pair<String, Number>> {
        val aggregatedData = mutableMapOf<String, MutableList<Float>>()

        data.forEach { (time, value) ->
            val heartRate = value.toFloat()
            if (heartRate > 0) {
                val hour = time.split(":")[0]
                val minutes = time.split(":")[1].toInt()

                val roundedMinutes = (minutes / 5) * 5
                val intervalKey = "$hour:${if (roundedMinutes < 10) "0$roundedMinutes" else roundedMinutes}"

                aggregatedData.getOrPut(intervalKey) { mutableListOf() }.add(heartRate)
            }
        }

        return aggregatedData.mapNotNull { (interval, values) ->
            if (values.isNotEmpty()) {
                interval to values.average().toFloat()
            } else {
                null
            }
        }.sortedBy { it.first }
    }

    private fun clearDatabase() {
        CoroutineScope(Dispatchers.IO).launch {
            fitnessDao.clearDatabase()
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
