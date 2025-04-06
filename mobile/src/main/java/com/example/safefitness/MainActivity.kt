package com.example.safefitness

import android.Manifest
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.NumberPicker
import android.widget.PopupMenu
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.example.safefitness.data.DataHandler
import com.example.safefitness.data.FitnessDatabase
import com.example.safefitness.data.FitnessRepository
import com.example.safefitness.data.WearDataListener
import com.example.safefitness.ui.graph.FullScreenGraphActivity
import com.example.safefitness.ui.main.MainViewModel
import com.example.safefitness.ui.main.MainViewModelFactory
import com.example.safefitness.ui.onboarding.OnboardingActivity
import com.example.safefitness.utils.DataExporter
import com.example.safefitness.utils.chart.GraphManager
import com.google.android.gms.wearable.Wearable
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.launch
import lecho.lib.hellocharts.view.LineChartView
import java.util.Locale

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
    private lateinit var dailyGoalProgress: LinearProgressIndicator
    private lateinit var loadingOverlay: View
    private lateinit var statusTextView: TextView
    private lateinit var wearDataListener: WearDataListener
    private val graphManager = GraphManager()
    private var lastPacketTimestamp = 0L
    private val PACKET_TIMEOUT_MS = 10000L
    private val checkSyncHandler = Handler(Looper.getMainLooper())
    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_SCAN
    )
    private val REQUEST_PERMISSIONS_CODE = 123
    private val REQUEST_ENABLE_BT = 456

    companion object {
        private const val PREFS_NAME = "goal_prefs"
        private const val KEY_GOAL = "step_goal"
        private const val DEFAULT_GOAL = 10000
        private const val KEY_LANG = "lang"
        private const val DEFAULT_LANG = "en"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        val prefsOnboarding = getSharedPreferences("onboarding_prefs", Context.MODE_PRIVATE)
        if (prefsOnboarding.getBoolean("isFirstLaunch", true)) {
            val intent = Intent(this, OnboardingActivity::class.java)
            ContextCompat.startActivity(this, intent, null)
            finish()
            return
        }

        loadLanguageFromPrefs()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        setSupportActionBar(topAppBar)

        initViews()

        if (!isGoalSet()) saveGoal(DEFAULT_GOAL)

        wearDataListener = WearDataListener(
            dataHandler,
            { updateData() },
            this,
            mainActivity = this
        )
        Wearable.getDataClient(this).addListener(wearDataListener)

        observeViewModel()
        requestPermissionsIfNeeded()

        stepsGraph.setOnClickListener { FullScreenGraphActivity.start(this, "steps") }
        heartRateGraph.setOnClickListener { FullScreenGraphActivity.start(this, "heartRate") }
    }

    private fun loadLanguageFromPrefs() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val langCode = prefs.getString(KEY_LANG, DEFAULT_LANG) ?: DEFAULT_LANG
        applyLocale(langCode)
    }

    private fun applyLocale(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        createConfigurationContext(config)
        resources.updateConfiguration(config, resources.displayMetrics)
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

        val rootLayout = findViewById<ConstraintLayout>(R.id.mainConstraintLayout)
        loadingOverlay = LayoutInflater.from(this).inflate(R.layout.loading_overlay, rootLayout, false)
        rootLayout.addView(loadingOverlay)
        loadingOverlay.visibility = View.GONE
        statusTextView = loadingOverlay.findViewById(R.id.statusTextView)
    }

    private fun requestPermissionsIfNeeded() {
        val toRequest = REQUIRED_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (toRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, toRequest.toTypedArray(), REQUEST_PERMISSIONS_CODE)
        } else {
            checkBluetooth()
            updateData()
        }
    }

    private fun checkBluetooth() {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        if (adapter != null) {
            if (!adapter.isEnabled) {
                showLoading(getString(R.string.loading_connecting))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                        != PackageManager.PERMISSION_GRANTED
                    ) {
                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                            REQUEST_PERMISSIONS_CODE
                        )
                        return
                    }
                }
                val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(intent, REQUEST_ENABLE_BT)
            } else {
                onBluetoothConnected()
            }
        } else {
            hideLoading()
        }
    }

    fun onBluetoothConnected() {
        lastPacketTimestamp = System.currentTimeMillis()
        showLoading(getString(R.string.loading_syncing))
        checkSynchronization()
    }

    fun allLargePacketsReceived(): Boolean {
        return (System.currentTimeMillis() - lastPacketTimestamp) > PACKET_TIMEOUT_MS
    }

    fun onDataPacketReceived() {
        lastPacketTimestamp = System.currentTimeMillis()
    }

    fun checkSynchronization() {
        val diff = System.currentTimeMillis() - lastPacketTimestamp
        android.util.Log.d("SyncCheck", "Time since last packet: $diff ms")
        if (allLargePacketsReceived()) {
            hideLoading()
        } else {
            checkSyncHandler.postDelayed({ checkSynchronization() }, 1000)
        }
    }


    fun showLoading(text: String) {
        loadingOverlay.visibility = View.VISIBLE
        statusTextView.text = text
    }

    fun hideLoading() {
        loadingOverlay.visibility = View.GONE
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, results)
        if (requestCode == REQUEST_PERMISSIONS_CODE) {
            checkBluetooth()
            updateData()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_BT) {
            val adapter = BluetoothAdapter.getDefaultAdapter()
            if (adapter != null && adapter.isEnabled) {
                onBluetoothConnected()
            } else {
                hideLoading()
            }
            updateData()
        }
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
        val popupMenu = PopupMenu(this, anchorView, Gravity.END)
        popupMenu.menuInflater.inflate(R.menu.popup_menu, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.popup_set_goal -> {
                    showSetGoalDialog()
                    true
                }
                R.id.popup_set_language -> {
                    showLanguageDialog()
                    true
                }
                R.id.popup_export_data -> {
                    lifecycleScope.launch {
                        try {
                            val file = DataExporter.exportData(this@MainActivity)
                            DataExporter.shareFile(this@MainActivity, file)
                        } catch (e: Exception) {
                            Toast.makeText(
                                this@MainActivity,
                                "Export error: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
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

    private fun showLanguageDialog() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentLang = prefs.getString(KEY_LANG, DEFAULT_LANG) ?: DEFAULT_LANG

        val dialogView = layoutInflater.inflate(R.layout.dialog_set_language, null)
        val radioGroup = dialogView.findViewById<RadioGroup>(R.id.languageRadioGroup)

        when (currentLang) {
            "en" -> radioGroup.check(R.id.radioEnglish)
            "sk" -> radioGroup.check(R.id.radioSlovak)
            "uk" -> radioGroup.check(R.id.radioUkrainian)
        }

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.language_dialog_title))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.goal_dialog_positive)) { dialog, _ ->
                when (radioGroup.checkedRadioButtonId) {
                    R.id.radioEnglish -> saveLanguage("en")
                    R.id.radioSlovak -> saveLanguage("sk")
                    R.id.radioUkrainian -> saveLanguage("uk")
                }
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.goal_dialog_negative)) { dialog, _ ->
                Toast.makeText(this, getString(R.string.language_canceled), Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun saveLanguage(languageCode: String) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANG, languageCode).apply()
        Toast.makeText(this, getString(R.string.language_saved, languageCode), Toast.LENGTH_SHORT).show()
        applyLocale(languageCode)
        recreate()
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

    private fun updateData() {
        mainViewModel.updateData(getCurrentGoal())
    }

    override fun onDestroy() {
        super.onDestroy()
        Wearable.getDataClient(this).removeListener(wearDataListener)
    }
}
