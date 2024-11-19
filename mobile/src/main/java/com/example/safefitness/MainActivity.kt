package com.example.safefitness

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.safefitness.data.DataHandler
import com.example.safefitness.data.FitnessDatabase
import com.example.safefitness.data.WearDataListener
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import lecho.lib.hellocharts.view.LineChartView
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var stepsGraph: LineChartView
    private lateinit var heartRateGraph: LineChartView
    private lateinit var totalStepsText: TextView
    private lateinit var lastHeartRateText: TextView

    private lateinit var dataHandler: DataHandler
    private lateinit var graphManager: GraphManager
    private lateinit var wearDataListener: WearDataListener

    private var aggregatedSteps: List<Pair<String, Number>> = listOf()
    private var aggregatedHeartRate: List<Pair<String, Number>> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()

        val database = FitnessDatabase.getDatabase(this)
        dataHandler = DataHandler(database.fitnessDao())
        graphManager = GraphManager()

        wearDataListener = WearDataListener(dataHandler) { updateData() }
        Wearable.getDataClient(this).addListener(wearDataListener)

        updateData()

        stepsGraph.setOnClickListener {
            openFullScreenGraph(
                aggregatedData = aggregatedSteps,
                title = "Steps",
                xAxisName = "Time",
                yAxisName = "Steps"
            )
        }

        heartRateGraph.setOnClickListener {
            openFullScreenGraph(
                aggregatedData = aggregatedHeartRate,
                title = "Heart Rate",
                xAxisName = "Time",
                yAxisName = "BPM"
            )
        }
    }

    private fun initViews() {
        stepsGraph = findViewById(R.id.stepsGraph)
        heartRateGraph = findViewById(R.id.heartRateGraph)
        totalStepsText = findViewById(R.id.totalStepsText)
        lastHeartRateText = findViewById(R.id.lastHeartRateText)
    }

    private fun updateData() {
        CoroutineScope(Dispatchers.IO).launch {
            val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val (stepsData, heartRateData) = dataHandler.getDailyAggregatedData(currentDate)

            aggregatedSteps = stepsData
            aggregatedHeartRate = heartRateData

            val totalSteps = dataHandler.fitnessDao.getTotalStepsForCurrentDay(currentDate)
            val lastHeartRate = dataHandler.fitnessDao.getLastHeartRateForCurrentDay(currentDate)

            runOnUiThread {
                totalStepsText.text = "Total Steps: $totalSteps"
                lastHeartRateText.text = "Last Heart Rate: ${lastHeartRate ?: "N/A"} bpm"

                graphManager.updateGraph(stepsGraph, aggregatedSteps, "Steps", "Time", "Steps", this@MainActivity)
                graphManager.updateGraph(heartRateGraph, aggregatedHeartRate, "Heart Rate", "Time", "BPM", this@MainActivity)
            }
        }
    }

    private fun openFullScreenGraph(
        aggregatedData: List<Pair<String, Number>>,
        title: String,
        xAxisName: String,
        yAxisName: String
    ) {
        val intent = Intent(this, FullScreenGraphActivity::class.java).apply {
            putExtra("graphData", ArrayList(aggregatedData)) // Передаємо дані графіка
            putExtra("title", title)
            putExtra("xAxisName", xAxisName)
            putExtra("yAxisName", yAxisName)
        }
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        Wearable.getDataClient(this).removeListener(wearDataListener)
    }
}
