package com.example.safefitness

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.safefitness.data.DataHandler
import com.example.safefitness.data.FitnessDao
import com.example.safefitness.data.FitnessDatabase
import com.example.safefitness.data.FitnessEntity
import com.example.safefitness.data.WearDataListener
import com.example.safefitness.ui.graph.FullScreenGraphActivity
import com.example.safefitness.utils.GraphManager
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

    private lateinit var avgHeartRateText: TextView
    private lateinit var minHeartRateText: TextView
    private lateinit var maxHeartRateText: TextView

    private lateinit var dataHandler: DataHandler
    private lateinit var graphManager: GraphManager
    private lateinit var wearDataListener: WearDataListener

    private var aggregatedSteps: List<Pair<String, Number>> = listOf()
    private var aggregatedHeartRate: List<Pair<String, Number>> = listOf()
    private lateinit var fitnessDao: FitnessDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

//        clearAllData()
//        populateDatabaseWithHourlyRandomDataFor2024()

        initViews()

        val database = FitnessDatabase.getDatabase(this)
        dataHandler = DataHandler(database.fitnessDao())
        graphManager = GraphManager()
        fitnessDao = database.fitnessDao()

        wearDataListener = WearDataListener(
            dataHandler = dataHandler,
            onDataUpdated = { updateData() },
            context = this
        )
        Wearable.getDataClient(this).addListener(wearDataListener)

        updateData()

        stepsGraph.setOnClickListener {
            FullScreenGraphActivity.start(this, "steps")
        }

        heartRateGraph.setOnClickListener {
            FullScreenGraphActivity.start(this, "heartRate")
        }
    }

    private fun initViews() {
        stepsGraph = findViewById(R.id.stepsGraph)
        heartRateGraph = findViewById(R.id.heartRateGraph)
        totalStepsText = findViewById(R.id.totalStepsText)
        lastHeartRateText = findViewById(R.id.lastHeartRateText)
        avgHeartRateText = findViewById(R.id.avgHeartRateText)
        minHeartRateText = findViewById(R.id.minHeartRateText)
        maxHeartRateText = findViewById(R.id.maxHeartRateText)
    }

    private fun updateData() {
        CoroutineScope(Dispatchers.IO).launch {
            val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val (stepsData, heartRateData) = dataHandler.getDailyAggregatedData(currentDate)

            aggregatedSteps = stepsData
            aggregatedHeartRate = heartRateData

            val totalSteps = fitnessDao.getTotalStepsForCurrentDay(currentDate)
            val lastHeartRate = fitnessDao.getLastHeartRateForCurrentDay(currentDate)
            val avgHeartRate = fitnessDao.getAverageHeartRateForCurrentDay(currentDate)
            val minHeartRate = fitnessDao.getMinHeartRateForCurrentDay(currentDate)
            val maxHeartRate = fitnessDao.getMaxHeartRateForCurrentDay(currentDate)

            runOnUiThread {
                totalStepsText.text = "Total Steps: $totalSteps"
                lastHeartRateText.text = "Last Heart Rate: ${lastHeartRate ?: "N/A"} bpm"

                avgHeartRateText.text = "Avg: ${avgHeartRate?.toInt() ?: "N/A"} bpm"
                minHeartRateText.text = "Min: ${minHeartRate?.toInt() ?: "N/A"} bpm"
                maxHeartRateText.text = "Max: ${maxHeartRate?.toInt() ?: "N/A"} bpm"

                graphManager.updateGraph(stepsGraph, aggregatedSteps, "Steps", "Time", "Steps", this@MainActivity)
                graphManager.updateGraph(heartRateGraph, aggregatedHeartRate, "Heart Rate", "Time", "BPM", this@MainActivity)
            }
        }
    }

    private fun openFullScreenGraph(
        dataType: String,
        aggregatedData: List<Pair<String, Number>>,
        title: String,
        xAxisName: String,
        yAxisName: String
    ) {
        val intent = Intent(this, FullScreenGraphActivity::class.java).apply {
            putExtra("graphData", ArrayList(aggregatedData))
            putExtra("dataType", dataType)
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

    private fun populateDatabaseWithHourlyRandomDataFor2024() {
        CoroutineScope(Dispatchers.IO).launch {
            val fitnessDao = FitnessDatabase.getDatabase(this@MainActivity).fitnessDao()

            val calendar = Calendar.getInstance().apply {
                set(Calendar.YEAR, 2024)
                set(Calendar.MONTH, Calendar.JANUARY)
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }

            val endDate = Calendar.getInstance().apply {
                set(Calendar.YEAR, 2024)
                set(Calendar.MONTH, Calendar.NOVEMBER)
                set(Calendar.DAY_OF_MONTH, 26)
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
            }

            var lastHeartRate = (40..50).random().toFloat()
            var hourlySteps = 0

            while (calendar <= endDate) {
                val hour = calendar.get(Calendar.HOUR_OF_DAY)
                val minute = calendar.get(Calendar.MINUTE)
                val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(calendar.time)

                if (minute == 0) {
                    hourlySteps = if (hour in 6..21) (0..1000).random() else 0
                }

                lastHeartRate = when {
                    hour in 0..5 || hour in 22..23 -> lastHeartRate + (-1..1).random()
                    hour in 6..8 || hour in 20..21 -> (50..70).random().toFloat()
                    hourlySteps > 700 -> (110..150).random().toFloat()
                    else -> lastHeartRate + (-3..3).random()
                }.coerceIn(40f, 150f)

                val exists = fitnessDao.dataExists(date, hourlySteps, lastHeartRate)
                if (exists == 0) {
                    fitnessDao.insertData(
                        FitnessEntity(
                            date = date,
                            steps = if (minute == 0) hourlySteps else 0,
                            heartRate = lastHeartRate
                        )
                    )
                }

                calendar.add(Calendar.MINUTE, 5)
            }
        }
    }

    private fun clearAllData() {
        CoroutineScope(Dispatchers.IO).launch {
            val dao = FitnessDatabase.getDatabase(this@MainActivity).fitnessDao()
            dao.clearDatabase()
            runOnUiThread {
                Toast.makeText(this@MainActivity, "All data has been cleared!", Toast.LENGTH_SHORT).show()
            }
        }
    }

}
