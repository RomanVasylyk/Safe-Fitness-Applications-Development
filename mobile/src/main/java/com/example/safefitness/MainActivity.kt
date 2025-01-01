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
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Observer
import com.example.safefitness.data.DataHandler
import com.example.safefitness.data.FitnessDatabase
import com.example.safefitness.data.FitnessRepository
import com.example.safefitness.data.WearDataListener
import com.example.safefitness.ui.graph.FullScreenGraphActivity
import com.example.safefitness.ui.main.MainViewModel
import com.example.safefitness.ui.main.MainViewModelFactory
import com.example.safefitness.utils.chart.GraphManager
import com.google.android.gms.wearable.Wearable
import com.google.android.material.appbar.MaterialToolbar
import lecho.lib.hellocharts.view.LineChartView

class MainActivity : AppCompatActivity() {

    private val database by lazy { FitnessDatabase.getDatabase(this) }
    private val repository by lazy { FitnessRepository(database.fitnessDao()) }
    private val dataHandler by lazy { DataHandler(database.fitnessDao()) }
    private val mainViewModel: MainViewModel by viewModels {
        MainViewModelFactory(repository, dataHandler, database.fitnessDao())
    }

    private lateinit var stepsGraph: LineChartView
    private lateinit var heartRateGraph: LineChartView
    private lateinit var totalStepsText: TextView
    private lateinit var lastHeartRateText: TextView
    private lateinit var avgHeartRateText: TextView
    private lateinit var minHeartRateText: TextView
    private lateinit var maxHeartRateText: TextView
    private lateinit var dailyGoalTitle: TextView
    private lateinit var dailyGoalProgress: com.google.android.material.progressindicator.LinearProgressIndicator
    private lateinit var wearDataListener: WearDataListener
    private val graphManager = GraphManager()

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
        initViews()
        if (!isGoalSet()) saveGoal(DEFAULT_GOAL)
        wearDataListener = WearDataListener(dataHandler, { updateData() }, this)
        Wearable.getDataClient(this).addListener(wearDataListener)
        observeViewModel()
        updateData()
        stepsGraph.setOnClickListener { FullScreenGraphActivity.start(this, "steps") }
        heartRateGraph.setOnClickListener { FullScreenGraphActivity.start(this, "heartRate") }
    }

    private fun initViews() {
        stepsGraph = findViewById(R.id.stepsGraph)
        heartRateGraph = findViewById(R.id.heartRateGraph)
        totalStepsText = findViewById(R.id.totalStepsText)
        lastHeartRateText = findViewById(R.id.lastHeartRateText)
        avgHeartRateText = findViewById(R.id.avgHeartRateText)
        minHeartRateText = findViewById(R.id.minHeartRateText)
        maxHeartRateText = findViewById(R.id.maxHeartRateText)
        dailyGoalTitle = findViewById(R.id.dailyGoalTitle)
        dailyGoalProgress = findViewById(R.id.dailyGoalProgress)
    }

    private fun observeViewModel() {
        mainViewModel.aggregatedSteps.observe(this, Observer {
            graphManager.updateGraph(
                stepsGraph,
                it,
                getString(R.string.steps),
                getString(R.string.time),
                getString(R.string.steps),
                this
            )
        })
        mainViewModel.aggregatedHeartRate.observe(this, Observer {
            graphManager.updateGraph(
                heartRateGraph,
                it,
                getString(R.string.heart_rate_label),
                getString(R.string.time),
                getString(R.string.bpm),
                this
            )
        })
        mainViewModel.totalSteps.observe(this, Observer {
            val str = getString(R.string.total_steps, it)
            totalStepsText.text = str
            updateGoalProgress(it)
        })
        mainViewModel.lastHeartRate.observe(this, Observer {
            val str = if (it != null) {
                getString(R.string.last_heart_rate, it.toInt().toString())
            } else getString(R.string.last_heart_rate, "N/A")
            lastHeartRateText.text = str
        })
        mainViewModel.avgHeartRate.observe(this, Observer {
            val str = getString(R.string.avg_heart_rate, it.toInt().toString())
            avgHeartRateText.text = str
        })
        mainViewModel.minHeartRate.observe(this, Observer {
            val str = if (it != null) {
                getString(R.string.min_heart_rate, it.toInt().toString())
            } else getString(R.string.min_heart_rate, "N/A")
            minHeartRateText.text = str
        })
        mainViewModel.maxHeartRate.observe(this, Observer {
            val str = if (it != null) {
                getString(R.string.max_heart_rate, it.toInt().toString())
            } else getString(R.string.max_heart_rate, "N/A")
            maxHeartRateText.text = str
        })
    }

    private fun updateData() {
        mainViewModel.updateData(getCurrentGoal())
    }

    private fun updateGoalProgress(steps: Int) {
        dailyGoalProgress.max = getCurrentGoal()
        if (steps >= getCurrentGoal()) {
            dailyGoalTitle.text = getString(R.string.goal_achieved, steps, getCurrentGoal())
            dailyGoalProgress.setProgress(getCurrentGoal(), true)
            val successColor = getColor(R.color.successColor)
            dailyGoalProgress.setIndicatorColor(successColor)
            dailyGoalTitle.setTextColor(successColor)
        } else {
            dailyGoalTitle.text = getString(R.string.daily_goal_1, steps, getCurrentGoal())
            dailyGoalProgress.setProgress(steps, true)
            val primaryColor = getColor(R.color.primaryColor)
            dailyGoalProgress.setIndicatorColor(primaryColor)
            dailyGoalTitle.setTextColor(primaryColor)
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
        val currentGoalIndex = goals.indexOfFirst { it == getCurrentGoal() }.takeIf { it >= 0 } ?: 0
        val dialogView = layoutInflater.inflate(R.layout.dialog_set_goal, null)
        val numberPicker = dialogView.findViewById<NumberPicker>(R.id.goalNumberPicker)
        numberPicker.minValue = 0
        numberPicker.maxValue = goals.size - 1
        numberPicker.displayedValues = goals.map { it.toString() }.toTypedArray()
        numberPicker.value = currentGoalIndex

        val builder = AlertDialog.Builder(this)
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
}
