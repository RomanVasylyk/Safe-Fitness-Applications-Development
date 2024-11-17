package com.example.safefitness

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.example.safefitness.databinding.ActivityMainBinding

class MainActivity : Activity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var sensorManagerHelper: SensorManagerHelper
    private lateinit var permissionManager: PermissionManager
    private lateinit var dataSender: DataSender
    private lateinit var fitnessDatabaseHelper: FitnessDatabaseHelper
    private val handler = Handler(Looper.getMainLooper())

    private val syncInterval = 5_000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sensorManagerHelper = SensorManagerHelper(this)
        permissionManager = PermissionManager(this)
        dataSender = DataSender(this)
        fitnessDatabaseHelper = FitnessDatabaseHelper(this)

        permissionManager.requestPermissions()

        binding.btnStart.setOnClickListener {
            sensorManagerHelper.startListening()
            startSyncingData()
            binding.btnStop.isEnabled = true
            binding.btnStart.isEnabled = false
        }

        binding.btnStop.setOnClickListener {
            sensorManagerHelper.stopListening()
            stopSyncingData()
            binding.btnStop.isEnabled = false
            binding.btnStart.isEnabled = true
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
            dataSender.sendAllDataToPhone(fitnessDatabaseHelper)
            handler.postDelayed(this, syncInterval)
        }
    }
}
