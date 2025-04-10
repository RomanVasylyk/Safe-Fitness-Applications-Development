package com.example.safefitness.presentation

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.example.safefitness.data.local.db.FitnessDatabase
import com.example.safefitness.data.remote.WearDataSender
import com.example.safefitness.data.remote.WatchDataListener
import com.example.safefitness.data.repository.FitnessRepository
import com.example.safefitness.data.repository.FitnessRepositoryImpl
import com.example.safefitness.databinding.ActivityMainBinding
import com.example.safefitness.helpers.PermissionManager
import com.example.safefitness.service.SensorService
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class MainActivity : Activity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var dataClient: DataClient
    private lateinit var watchDataListener: WatchDataListener
    private lateinit var permissionManager: PermissionManager

    private lateinit var repository: FitnessRepository

    private val handler = Handler(Looper.getMainLooper())
    private val syncInterval = 5000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dataClient = Wearable.getDataClient(this)
        watchDataListener = WatchDataListener(this)
        dataClient.addListener(watchDataListener)

        permissionManager = PermissionManager(this)
        permissionManager.requestPermissions()

        val db = FitnessDatabase.getInstance(this)
        val fitnessDao = db.fitnessDao()
        val sentBatchDao = db.sentBatchDao()
        val dataSender = WearDataSender(this)
        repository = FitnessRepositoryImpl(fitnessDao, sentBatchDao, dataSender)

        deleteOldData()

        binding.btnStart.setOnClickListener { startTracking() }
        binding.btnStop.setOnClickListener { stopTracking() }
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
        val intent = Intent(this, SensorService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun stopSensorService() {
        val intent = Intent(this, SensorService::class.java)
        stopService(intent)
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
                repository.syncDataWithPhone()
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
            val todaySteps = repository.getStepsForCurrentDay(getCurrentDate()) ?: 0
            val heartRate = repository.getLastHeartRateForCurrentDay(getCurrentDate())
            runOnUiThread {
                binding.stepsText.text = todaySteps.toString()
                binding.heartText.text = if (heartRate != null) "${heartRate.toInt()} bpm" else "N/A"
            }
        }
    }

    private fun deleteOldData() {
        CoroutineScope(Dispatchers.IO).launch {
            val calendar = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -7)
            }
            val sevenDaysAgo = android.icu.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(calendar.time)
            repository.deleteOldData(sevenDaysAgo)
        }
    }

    private fun getCurrentDate(): String {
        return android.icu.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopSensorService()
        dataClient.removeListener(watchDataListener)
    }
}
