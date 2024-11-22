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
        wearDataListener = WearDataListener(dataHandler) { updateData() }
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
                set(Calendar.DAY_OF_MONTH, 22)
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
            }

            while (calendar <= endDate) {
                val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(calendar.time)

                val randomSteps = (0..1000).random()
                val randomHeartRate = (60..190).random().toFloat()

                val exists = fitnessDao.dataExists(date, randomSteps, randomHeartRate)
                if (exists == 0) {
                    fitnessDao.insertData(
                        FitnessEntity(
                            date = date,
                            steps = randomSteps,
                            heartRate = randomHeartRate
                        )
                    )
                }

                calendar.add(Calendar.HOUR_OF_DAY, 1)
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
