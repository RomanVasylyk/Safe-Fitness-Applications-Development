package com.example.safefitness.service

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
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.example.safefitness.R
import com.example.safefitness.data.local.db.FitnessDatabase
import com.example.safefitness.data.local.entity.FitnessEntity
import com.example.safefitness.data.repository.FitnessRepository
import com.example.safefitness.data.repository.FitnessRepositoryImpl
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.*

class SensorService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var heartRateSensor: Sensor? = null
    private var stepCounterSensor: Sensor? = null

    private val repository: FitnessRepository by lazy {
        val db = FitnessDatabase.getInstance(this)
        FitnessRepositoryImpl(
            db.fitnessDao(),
            db.sentBatchDao(),
            com.example.safefitness.data.remote.WearDataSender(this)
        )
    }

    private var wakeLock: PowerManager.WakeLock? = null

    private var initialSteps: Int? = null
    private var initialStepsDate: String? = null
    private var totalStepsForDay = 0

    private val stepBuffer = mutableListOf<Int>()
    private val heartRateBuffer = mutableListOf<Float>()
    private val stepMutex = Mutex()
    private val heartRateMutex = Mutex()
    private var isBufferingSteps = false
    private var isBufferingHeartRate = false

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
        stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        if (isDeviceRebooted()) {
            val prefs = getSharedPreferences("fitness_prefs", Context.MODE_PRIVATE)
            prefs.edit()
                .remove("initial_steps")
                .remove("initial_date")
                .remove("last_sensor_steps")
                .apply()
        }

        acquireWakeLock()
        startListening()
        startForeground(NOTIFICATION_ID, createNotification())
    }

    private fun startListening() {
        heartRateSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        stepCounterSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    private fun stopListening() {
        sensorManager.unregisterListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopListening()
        releaseWakeLock()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            when (it.sensor.type) {
                Sensor.TYPE_STEP_COUNTER -> handleStepCounter(it.values[0].toInt())
                Sensor.TYPE_HEART_RATE   -> handleHeartRate(it.values[0])
            }
        }
    }

    private fun handleStepCounter(currentSteps: Int) {
        val currentDate = getCurrentDate()

        val (savedInitial, savedDate) = getInitialSteps()
        val savedLastSensorSteps = getLastSensorSteps()

        if (savedInitial == null || savedDate != currentDate) {
            initialSteps = currentSteps
            initialStepsDate = currentDate
            saveInitialSteps(currentSteps, currentDate)
            totalStepsForDay = 0
            saveLastSensorSteps(currentSteps)
        } else {
            initialSteps = savedInitial
            initialStepsDate = savedDate
        }

        val lastSensorSteps = savedLastSensorSteps ?: initialSteps ?: currentSteps
        val increment = currentSteps - lastSensorSteps
        if (increment > 0) {
            totalStepsForDay += increment
            bufferSteps(increment)
            saveLastSensorSteps(currentSteps)
        }
    }

    private fun handleHeartRate(hrValue: Float) {
        serviceScope.launch {
            heartRateMutex.withLock {
                heartRateBuffer.add(hrValue)
            }
            if (!isBufferingHeartRate) {
                isBufferingHeartRate = true
                delay(HEART_BUFFER_DELAY)

                val averageHeartRate: Float?
                heartRateMutex.withLock {
                    averageHeartRate = if (heartRateBuffer.isNotEmpty()) {
                        heartRateBuffer.average().toFloat().also { heartRateBuffer.clear() }
                    } else null
                }
                averageHeartRate?.let {
                    saveHeartRateToDatabase(it)
                }
                isBufferingHeartRate = false
            }
        }
    }

    private fun bufferSteps(steps: Int) {
        serviceScope.launch {
            stepMutex.withLock {
                stepBuffer.add(steps)
            }
            if (!isBufferingSteps) {
                isBufferingSteps = true
                delay(STEP_BUFFER_DELAY)

                val totalBuffered: Int
                stepMutex.withLock {
                    totalBuffered = stepBuffer.sum()
                    stepBuffer.clear()
                }
                if (totalBuffered > 0) {
                    saveStepsToDatabase(totalBuffered)
                }
                isBufferingSteps = false
            }
        }
    }

    private fun saveStepsToDatabase(steps: Int) {
        val currentTime = getCurrentTime()
        serviceScope.launch {
            try {
                val entity = FitnessEntity(
                    date = currentTime,
                    steps = steps,
                    heartRate = null,
                    isSynced = false
                )
                repository.insertOrUpdateData(entity)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun saveHeartRateToDatabase(hr: Float) {
        val currentTime = getCurrentTime()
        serviceScope.launch {
            try {
                val rounded = hr.toInt().toFloat()
                val lastHR = repository.getLastHeartRateForCurrentDay(getCurrentDate())
                if (lastHR == null || lastHR != rounded) {
                    val entity = FitnessEntity(
                        date = currentTime,
                        steps = null,
                        heartRate = rounded,
                        isSynced = false
                    )
                    repository.insertOrUpdateData(entity)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun acquireWakeLock() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SafeFitness:WakeLock")
        wakeLock?.acquire()
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
    }

    private fun isDeviceRebooted(): Boolean {
        val prefs = getSharedPreferences("fitness_prefs", Context.MODE_PRIVATE)
        val lastUptime = prefs.getLong("last_uptime", -1)
        val currentUptime = SystemClock.elapsedRealtime()
        val rebooted = (lastUptime > currentUptime && lastUptime != -1L)
        prefs.edit().putLong("last_uptime", currentUptime).apply()
        return rebooted
    }

    private fun saveInitialSteps(steps: Int, date: String) {
        val prefs = getSharedPreferences("fitness_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("initial_steps", steps).putString("initial_date", date).apply()
    }

    private fun getInitialSteps(): Pair<Int?, String?> {
        val prefs = getSharedPreferences("fitness_prefs", Context.MODE_PRIVATE)
        val stp = prefs.getInt("initial_steps", -1).takeIf { it != -1 }
        val dt = prefs.getString("initial_date", null)
        return stp to dt
    }

    private fun saveLastSensorSteps(steps: Int) {
        val prefs = getSharedPreferences("fitness_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("last_sensor_steps", steps).apply()
    }

    private fun getLastSensorSteps(): Int? {
        val prefs = getSharedPreferences("fitness_prefs", Context.MODE_PRIVATE)
        val stp = prefs.getInt("last_sensor_steps", -1)
        return if (stp != -1) stp else null
    }

    private fun getCurrentTime(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
    }

    private fun getCurrentDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    private fun createNotification(): Notification {
        val channelId = "SENSOR_SERVICE_CHANNEL"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Sensor Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("SafeFitness")
            .setContentText("Collecting fitness data")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val STEP_BUFFER_DELAY = 5000L
        private const val HEART_BUFFER_DELAY = 5000L
    }
}
