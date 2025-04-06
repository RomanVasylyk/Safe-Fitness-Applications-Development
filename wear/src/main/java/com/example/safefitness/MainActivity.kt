package com.example.safefitness

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.example.safefitness.data.FitnessDao
import com.example.safefitness.data.FitnessDatabase
import com.example.safefitness.databinding.ActivityMainBinding
import com.example.safefitness.helpers.PermissionManager
import com.example.safefitness.helpers.SensorService
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class MainActivity : Activity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var fitnessDao: FitnessDao
    private lateinit var dataSender: DataSender
    private lateinit var permissionManager: PermissionManager
    private lateinit var dataClient: DataClient
    private lateinit var watchDataListener: WatchDataListener
    private val handler = Handler(Looper.getMainLooper())
    private val syncInterval = 5000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeComponents()
        setupListeners()
    }

    private fun initializeComponents() {
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dataClient = Wearable.getDataClient(this)
        watchDataListener = WatchDataListener(this)

        val database = FitnessDatabase.getDatabase(this)
        fitnessDao = database.fitnessDao()
        dataSender = DataSender(this)

        permissionManager = PermissionManager(this)
        permissionManager.requestPermissions()

        dataClient.addListener(watchDataListener)
        deleteOldData()
    }

    private fun setupListeners() {
        binding.btnStart.setOnClickListener {
            startTracking()
        }

        binding.btnStop.setOnClickListener {
            stopTracking()
        }
    }

    private fun startTracking() {
        startSensorService()
        startSyncingData()
        startUpdatingSteps()
        updateUI(true)
        showToast("Started Tracking")
    }

    private fun stopTracking() {
        stopSensorService()
        stopSyncingData()
        stopUpdatingSteps()
        updateUI(false)
        showToast("Stopped Tracking")
    }

    private fun updateUI(isTracking: Boolean) {
        binding.btnStart.isEnabled = !isTracking
        binding.btnStop.isEnabled = isTracking
    }

    private fun startSensorService() {
        val serviceIntent = Intent(this, SensorService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun stopSensorService() {
        val serviceIntent = Intent(this, SensorService::class.java)
        stopService(serviceIntent)
    }

    private fun startSyncingData() {
        handler.post(syncRunnable)
        showToast("Data syncing started")
    }

    private fun stopSyncingData() {
        handler.removeCallbacks(syncRunnable)
        showToast("Data syncing stopped")
    }

    private val syncRunnable = object : Runnable {
        override fun run() {
            CoroutineScope(Dispatchers.IO).launch {
                dataSender.sendUnsyncedDataToPhone()
            }
            handler.postDelayed(this, syncInterval)
        }
    }

    private fun startUpdatingSteps() {
        handler.post(updateStepsRunnable)
    }

    private fun stopUpdatingSteps() {
        handler.removeCallbacks(updateStepsRunnable)
    }

    private val updateStepsRunnable = object : Runnable {
        override fun run() {
            updateSteps()
            handler.postDelayed(this, syncInterval)
        }
    }

    private fun updateSteps() {
        CoroutineScope(Dispatchers.IO).launch {
            val todaySteps = fitnessDao.getStepsForCurrentDay(getCurrentDate())
            val heartRate = fitnessDao.getLastHeartRateForCurrentDay(getCurrentDate())
            runOnUiThread {
                binding.stepsText.text = todaySteps?.toString() ?: "0"
                binding.heartText.text = "${heartRate?.toInt() ?: "N/A"} bpm"
            }
        }
    }

    private fun deleteOldData() {
        CoroutineScope(Dispatchers.IO).launch {
            val calendar = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -7)
            }
            val sevenDaysAgo = android.icu.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
            fitnessDao.deleteOldData(sevenDaysAgo)
            val olderThanMillis = calendar.timeInMillis
            FitnessDatabase.getDatabase(this@MainActivity).sentBatchDao().deleteOldConfirmedBatches(olderThanMillis)
        }
    }

    private fun getCurrentDate(): String {
        return android.icu.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopSensorService()
        dataClient.removeListener(watchDataListener)
    }
}
