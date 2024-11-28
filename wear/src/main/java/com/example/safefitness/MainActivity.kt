package com.example.safefitness

import android.icu.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.example.safefitness.data.FitnessDao
import com.example.safefitness.data.FitnessDatabase
import com.example.safefitness.databinding.ActivityMainBinding
import com.example.safefitness.helpers.PermissionManager
import com.example.safefitness.helpers.SensorManagerHelper
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class MainActivity : Activity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var sensorManagerHelper: SensorManagerHelper
    private lateinit var dataSender: DataSender
    private lateinit var fitnessDao: FitnessDao
    private val handler = Handler(Looper.getMainLooper())
    private val syncInterval = 5000L
    private lateinit var permissionManager: PermissionManager
    private lateinit var confirmationListener: ConfirmationListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val database = FitnessDatabase.getDatabase(this)
        fitnessDao = database.fitnessDao()
        sensorManagerHelper = SensorManagerHelper(this)
        dataSender = DataSender(this)
        permissionManager = PermissionManager(this)
        permissionManager.requestPermissions()

        val fitnessDao = FitnessDatabase.getDatabase(this).fitnessDao()
        confirmationListener = ConfirmationListener(fitnessDao)

        Wearable.getMessageClient(this).addListener(confirmationListener)

        deleteOldData()

        binding.btnStart.setOnClickListener {
            sensorManagerHelper.startListening()
            startSyncingData()
            startUpdatingSteps()
            binding.btnStart.isEnabled = false
            binding.btnStop.isEnabled = true
            Toast.makeText(this, "Started Tracking", Toast.LENGTH_SHORT).show()
        }

        binding.btnStop.setOnClickListener {
            sensorManagerHelper.stopListening()
            stopSyncingData()
            stopUpdatingSteps()
            binding.btnStart.isEnabled = true
            binding.btnStop.isEnabled = false
            Toast.makeText(this, "Stopped Tracking", Toast.LENGTH_SHORT).show()
        }

        sensorManagerHelper.onHeartRateChanged = { heartRate ->
            runOnUiThread {
                binding.heartText.text = "${heartRate.toInt()} bpm"
            }
        }
    }

    private fun startSyncingData() {
        handler.post(syncRunnable)
        Toast.makeText(this, "Data syncing started", Toast.LENGTH_SHORT).show()
    }

    private fun stopSyncingData() {
        handler.removeCallbacks(syncRunnable)
        Toast.makeText(this, "Data syncing stopped", Toast.LENGTH_SHORT).show()
    }

    private val syncRunnable = object : Runnable {
        override fun run() {
            CoroutineScope(Dispatchers.IO).launch {
                dataSender.sendAllDataToPhone()
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
            runOnUiThread {
                binding.stepsText.text = todaySteps?.toString() ?: "0"
            }
        }
    }

    private fun getCurrentDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    private fun deleteOldData() {
        CoroutineScope(Dispatchers.IO).launch {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -7)
            val sevenDaysAgo = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
            fitnessDao.deleteOldData(sevenDaysAgo)
        }
    }
}
