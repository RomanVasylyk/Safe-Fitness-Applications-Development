package com.example.safefitness

import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.NumberPicker
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.safefitness.data.DataHandler
import com.example.safefitness.data.FitnessDao
import com.example.safefitness.data.FitnessDatabase
import com.example.safefitness.data.FitnessEntity
import com.example.safefitness.data.WearDataListener
import com.example.safefitness.ui.graph.FullScreenGraphActivity
import com.example.safefitness.utils.chart.GraphManager
import com.google.android.gms.wearable.Wearable
import com.google.android.material.appbar.MaterialToolbar
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

    companion object {
        private const val PREFS_NAME = "goal_prefs"
        private const val KEY_GOAL = "step_goal"
        private const val DEFAULT_GOAL = 10000
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        setSupportActionBar(topAppBar)

//        clearAllData()
//        populateDatabaseWithHourlyRandomDataFor2024()

        initViews()

        val database = FitnessDatabase.getDatabase(this)
        dataHandler = DataHandler(database.fitnessDao())
        graphManager = GraphManager()
        fitnessDao = database.fitnessDao()

        if (!isGoalSet()) {
            saveGoal(DEFAULT_GOAL)
        }

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

            aggregatedSteps = stepsData.sortedBy { it.first }
            aggregatedHeartRate = heartRateData.sortedBy { it.first }

            val totalSteps = fitnessDao.getTotalStepsForCurrentDay(currentDate)
            val lastHeartRate = fitnessDao.getLastHeartRateForCurrentDay(currentDate)
            val avgHeartRate = fitnessDao.getAverageHeartRateForCurrentDay(currentDate)
            val minHeartRate = fitnessDao.getMinHeartRateForCurrentDay(currentDate)
            val maxHeartRate = fitnessDao.getMaxHeartRateForCurrentDay(currentDate)

            val currentGoal = getCurrentGoal()

            runOnUiThread {
                val totalStepsStr = getString(R.string.total_steps, totalSteps)
                val lastHeartRateStr = if (lastHeartRate != null) {
                    getString(R.string.last_heart_rate, lastHeartRate.toInt().toString())
                } else {
                    getString(R.string.last_heart_rate, "N/A")
                }
                val avgHeartRateStr = if (avgHeartRate != null) {
                    getString(R.string.avg_heart_rate, avgHeartRate.toInt().toString())
                } else {
                    getString(R.string.avg_heart_rate, "N/A")
                }
                val minHeartRateStr = if (minHeartRate != null) {
                    getString(R.string.min_heart_rate, minHeartRate.toInt().toString())
                } else {
                    getString(R.string.min_heart_rate, "N/A")
                }
                val maxHeartRateStr = if (maxHeartRate != null) {
                    getString(R.string.max_heart_rate, maxHeartRate.toInt().toString())
                } else {
                    getString(R.string.max_heart_rate, "N/A")
                }

                totalStepsText.text = totalStepsStr
                lastHeartRateText.text = lastHeartRateStr
                avgHeartRateText.text = avgHeartRateStr
                minHeartRateText.text = minHeartRateStr
                maxHeartRateText.text = maxHeartRateStr

                graphManager.updateGraph(
                    stepsGraph,
                    aggregatedSteps,
                    getString(R.string.steps),
                    getString(R.string.time),
                    getString(R.string.steps),
                    this@MainActivity
                )
                graphManager.updateGraph(
                    heartRateGraph,
                    aggregatedHeartRate,
                    getString(R.string.heart_rate_label),
                    getString(R.string.time),
                    getString(R.string.bpm),
                    this@MainActivity
                )

                val dailyGoalTitle = findViewById<TextView>(R.id.dailyGoalTitle)
                val dailyGoalProgress =
                    findViewById<com.google.android.material.progressindicator.LinearProgressIndicator>(
                        R.id.dailyGoalProgress
                    )
                dailyGoalProgress.max = currentGoal

                if (totalSteps >= currentGoal) {
                    dailyGoalTitle.text = getString(R.string.goal_achieved, totalSteps, currentGoal)
                    dailyGoalProgress.setProgress(currentGoal, true)
                    val successColor = getColor(R.color.successColor)
                    dailyGoalProgress.setIndicatorColor(successColor)
                    dailyGoalTitle.setTextColor(successColor)
                } else {
                    dailyGoalTitle.text = getString(R.string.daily_goal_1, totalSteps, currentGoal)
                    dailyGoalProgress.setProgress(totalSteps, true)
                    val primaryColor = getColor(R.color.primaryColor)
                    dailyGoalProgress.setIndicatorColor(primaryColor)
                    dailyGoalTitle.setTextColor(primaryColor)
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                showPopupMenu()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showPopupMenu() {
        val anchorView = findViewById<View>(R.id.topAppBar)
        val popupMenu = PopupMenu(this, anchorView, android.view.Gravity.END)
        popupMenu.menuInflater.inflate(R.menu.popup_menu, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.popup_set_goal -> {
                    showSetGoalDialog()
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun showSetGoalDialog() {
        val goals = (1..50).map { it * 1000 }.toTypedArray()

        val currentGoal = getCurrentGoal()
        val currentGoalIndex = goals.indexOfFirst { it == currentGoal }.takeIf { it >= 0 } ?: 0

        val dialogView = layoutInflater.inflate(R.layout.dialog_set_goal, null)
        val numberPicker = dialogView.findViewById<NumberPicker>(R.id.goalNumberPicker)

        numberPicker.minValue = 0
        numberPicker.maxValue = goals.size - 1
        numberPicker.displayedValues = goals.map { it.toString() }.toTypedArray()
        numberPicker.wrapSelectorWheel = false
        numberPicker.value = currentGoalIndex

        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.goal_dialog_title))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.goal_dialog_positive)) { dialog, _ ->
                val selectedGoal = goals[numberPicker.value]
                saveGoal(selectedGoal)
                Toast.makeText(
                    this,
                    getString(R.string.goal_selected_message, selectedGoal),
                    Toast.LENGTH_SHORT
                ).show()
                dialog.dismiss()
                updateData()
            }
            .setNegativeButton(getString(R.string.goal_dialog_negative)) { dialog, _ ->
                dialog.dismiss()
            }

        builder.create().show()
    }

    private fun saveGoal(goal: Int) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_GOAL, goal).apply()
    }

    private fun getCurrentGoal(): Int {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_GOAL, DEFAULT_GOAL)
    }

    private fun isGoalSet(): Boolean {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.contains(KEY_GOAL)
    }

    override fun onDestroy() {
        super.onDestroy()
        Wearable.getDataClient(this).removeListener(wearDataListener)
    }

    private fun populateDatabaseWithHourlyRandomDataFor2024() {
        CoroutineScope(Dispatchers.IO).launch {
            val fitnessDao = FitnessDatabase.getDatabase(this@MainActivity).fitnessDao()

            val calendar = Calendar.getInstance().apply {
                set(Calendar.YEAR, 2023)
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
