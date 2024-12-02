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
import androidx.core.app.NotificationCompat
import com.example.safefitness.R
import com.example.safefitness.data.FitnessDatabase
import com.example.safefitness.data.FitnessEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private var initialStepsDate: String? = null
    private var totalStepsForDay = 0

    private val stepBuffer = mutableListOf<Int>()
    private val heartRateBuffer = mutableListOf<Float>()

    private val stepMutex = Mutex()
    private val heartRateMutex = Mutex()

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
        stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        startListening()
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
        serviceScope.cancel()
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
                    val currentDate = getCurrentDate()

                    val (savedInitialSteps, savedDate) = getInitialSteps()

                    if (savedInitialSteps == null || savedDate != currentDate) {
                        initialSteps = currentSteps
                        initialStepsDate = currentDate
                        saveInitialSteps(initialSteps!!, initialStepsDate!!)
                        totalStepsForDay = 0
                    } else {
                        initialSteps = savedInitialSteps
                        initialStepsDate = savedDate
                    }

                    val todaySteps = currentSteps - (initialSteps ?: 0)
                    val addedSteps = todaySteps - totalStepsForDay

                    if (addedSteps > 0) {
                        totalStepsForDay += addedSteps
                        bufferSteps(addedSteps)
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
        serviceScope.launch {
            heartRateMutex.withLock {
                heartRateBuffer.add(heartRate)
            }
            delay(5000)

            val averageHeartRate: Float?
            heartRateMutex.withLock {
                if (heartRateBuffer.isNotEmpty()) {
                    averageHeartRate = heartRateBuffer.average().toFloat()
                    heartRateBuffer.clear()
                } else {
                    averageHeartRate = null
                }
            }

            averageHeartRate?.let {
                saveHeartRateToDatabase(it)
            }
        }
    }

    private fun bufferSteps(steps: Int) {
        serviceScope.launch {
            stepMutex.withLock {
                stepBuffer.add(steps)
            }
            delay(5000)

            val totalBufferedSteps: Int
            stepMutex.withLock {
                totalBufferedSteps = stepBuffer.sum()
                stepBuffer.clear()
            }

            if (totalBufferedSteps > 0) {
                saveStepsToDatabase(totalBufferedSteps)
            }
        }
    }

    private fun saveStepsToDatabase(steps: Int) {
        val currentTime = getCurrentTime()
        serviceScope.launch {
            try {
                val exists = fitnessDao.dataExists(currentTime, steps, null)
                if (exists == 0) {
                    fitnessDao.insertData(
                        FitnessEntity(
                            date = currentTime,
                            steps = steps,
                            heartRate = null,
                            isSynced = false
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun saveHeartRateToDatabase(heartRate: Float) {
        val currentTime = getCurrentTime()
        serviceScope.launch {
            try {
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
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getCurrentTime(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
    }

    private fun getCurrentDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun saveInitialSteps(steps: Int, date: String) {
        val prefs = getSharedPreferences("fitness_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("initial_steps", steps).putString("initial_date", date).apply()
    }

    private fun getInitialSteps(): Pair<Int?, String?> {
        val prefs = getSharedPreferences("fitness_prefs", Context.MODE_PRIVATE)
        val steps = prefs.getInt("initial_steps", -1).takeIf { it != -1 }
        val date = prefs.getString("initial_date", null)
        return Pair(steps, date)
    }
}
