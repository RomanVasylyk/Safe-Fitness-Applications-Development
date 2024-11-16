package com.example.safefitness

import android.app.Activity
import android.os.Bundle
import android.util.Log
import com.example.safefitness.databinding.ActivityMainBinding

class MainActivity : Activity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var sensorManagerHelper: SensorManagerHelper
    private lateinit var permissionManager: PermissionManager
    private lateinit var dataSender: DataSender

    private var startTime: Long = 0
    private var endTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sensorManagerHelper = SensorManagerHelper(this)
        permissionManager = PermissionManager(this)
        dataSender = DataSender(this)

        sensorManagerHelper.onHeartRateChanged = { heartRate ->
            binding.heartText.text = "${heartRate.toInt()} bpm"
            dataSender.sendData("heartRate", heartRate)
        }
        sensorManagerHelper.onStepCountChanged = { steps ->
            binding.stepsText.text = "$steps steps"
            dataSender.sendData("steps", steps.toFloat())
        }

        permissionManager.requestPermissions()

        binding.btnStart.setOnClickListener {
            sensorManagerHelper.startListening()
            startTime = System.currentTimeMillis()
            binding.btnStop.isEnabled = true
            binding.btnStart.isEnabled = false
        }

        binding.btnStop.setOnClickListener {
            sensorManagerHelper.stopListening()
            binding.btnStop.isEnabled = false
            binding.btnStart.isEnabled = true

            endTime = System.currentTimeMillis()
            val durationMinutes = (endTime - startTime) / (1000 * 60)
            Log.d("MainActivity", "Training duration: $durationMinutes minutes")
        }
    }
}
