package com.example.safefitness.helpers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.safefitness.R
import com.example.safefitness.data.FitnessDatabase
import com.example.safefitness.data.FitnessEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SensorService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var heartRateSensor: Sensor? = null
    private var stepCounterSensor: Sensor? = null

    private val fitnessDao by lazy {
        FitnessDatabase.getDatabase(this).fitnessDao()
    }

    private var initialSteps: Int? = null
    private var totalStepsForDay = 0
    private var stepBuffer = 0
    private var isBuffering = false
    private var lastSensorStepCount: Int? = null
    private var lastStepUpdateTime: Long = System.currentTimeMillis()
    private val heartRateBuffer = mutableListOf<Float>()
    private var isHeartRateBuffering = false

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
        stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        startListening()
        validateStepCount()
    }

    private fun startListening() {
        heartRateSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        stepCounterSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
    }

    private fun stopListening() {
        sensorManager.unregisterListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopListening()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        startForeground(1, notification)
        return START_STICKY
    }

    private fun createNotification(): Notification {
        val notificationChannelId = "SENSOR_SERVICE_CHANNEL"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                notificationChannelId,
                "Sensor Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notificationBuilder = NotificationCompat.Builder(this, notificationChannelId)
        return notificationBuilder
            .setContentTitle("SafeFitness")
            .setContentText("Collecting fitness data")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            when (it.sensor.type) {
                Sensor.TYPE_STEP_COUNTER -> {
                    val currentSteps = it.values[0].toInt()
                    if (lastSensorStepCount != currentSteps) {
                        lastSensorStepCount = currentSteps
                        lastStepUpdateTime = System.currentTimeMillis()
                    }

                    if (initialSteps == null) {
                        initialSteps = currentSteps
                        saveInitialSteps(initialSteps!!)
                    } else {
                        initialSteps = getInitialSteps()
                    }

                    val todaySteps = currentSteps - (initialSteps ?: 0)
                    val addedSteps = todaySteps - totalStepsForDay

                    if (addedSteps > 0) {
                        bufferSteps(addedSteps)
                        totalStepsForDay += addedSteps
                    }
                }
                Sensor.TYPE_HEART_RATE -> {
                    val heartRate = it.values[0]
                    bufferHeartRate(heartRate)
                }
                else -> {}
            }
        }
    }

    private fun bufferHeartRate(heartRate: Float) {
        synchronized(heartRateBuffer) {
            heartRateBuffer.add(heartRate)
        }

        if (!isHeartRateBuffering) {
            isHeartRateBuffering = true
            CoroutineScope(Dispatchers.IO).launch {
                delay(5000)
                synchronized(heartRateBuffer) {
                    if (heartRateBuffer.isNotEmpty()) {
                        val averageHeartRate = heartRateBuffer.average().toFloat()
                        saveHeartRateToDatabase(averageHeartRate)
                        heartRateBuffer.clear()
                    }
                    isHeartRateBuffering = false
                }
            }
        }
    }

    private fun bufferSteps(steps: Int) {
        stepBuffer += steps

        if (!isBuffering) {
            isBuffering = true
            CoroutineScope(Dispatchers.IO).launch {
                delay(5000)
                if (stepBuffer > 0) {
                    saveStepsToDatabase(stepBuffer)
                    stepBuffer = 0
                }
                isBuffering = false
            }
        }
    }

    private fun validateStepCount() {
        CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                delay(300000)
                val currentTime = System.currentTimeMillis()
                val elapsedTime = currentTime - lastStepUpdateTime

                if (elapsedTime >= 300000) {
                    try {
                        val currentSteps = lastSensorStepCount ?: return@launch
                        val todaySteps = currentSteps - (getInitialSteps() ?: 0)
                        val savedSteps = fitnessDao.getStepsForCurrentDay(getCurrentTime()) ?: 0
                        val missingSteps = todaySteps - savedSteps

                        if (missingSteps > 0) {
                            Log.d("SensorService", "Found missing steps after 5 min idle: $missingSteps")
                            saveStepsToDatabase(missingSteps)
                        }
                    } catch (e: Exception) {
                        Log.e("SensorService", "Error in step validation: ${e.message}", e)
                    }
                }
            }
        }
    }


    private fun saveStepsToDatabase(steps: Int) {
        val currentTime = getCurrentTime()
        CoroutineScope(Dispatchers.IO).launch {
            val exists = fitnessDao.dataExists(currentTime, steps, null)
            if (exists == 0) {
                fitnessDao.insertData(FitnessEntity(date = currentTime, steps = steps, heartRate = null, isSynced = false))
            }
        }
    }

    private fun saveHeartRateToDatabase(heartRate: Float) {
        val currentTime = getCurrentTime()
        CoroutineScope(Dispatchers.IO).launch {
            val existingEntry = fitnessDao.getEntryByDate(currentTime)
            if (existingEntry != null) {
                if (existingEntry.heartRate != heartRate) {
                    fitnessDao.updateHeartRateByTime(currentTime, heartRate)
                }
            } else {
                fitnessDao.insertData(
                    FitnessEntity(
                        date = currentTime,
                        steps = null,
                        heartRate = heartRate,
                        isSynced = false
                    )
                )
            }
        }
    }

    private fun getCurrentTime(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun saveInitialSteps(steps: Int) {
        val prefs = getSharedPreferences("fitness_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("initial_steps", steps).apply()
    }

    private fun getInitialSteps(): Int? {
        val prefs = getSharedPreferences("fitness_prefs", Context.MODE_PRIVATE)
        return prefs.getInt("initial_steps", -1).takeIf { it != -1 }
    }
}
