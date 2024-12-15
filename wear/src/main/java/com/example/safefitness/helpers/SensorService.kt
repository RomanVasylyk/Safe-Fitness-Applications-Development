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
import android.os.PowerManager
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

    private var wakeLock: PowerManager.WakeLock? = null

    private var initialSteps: Int? = null
    private var initialStepsDate: String? = null
    private var totalStepsForDay = 0

    private val stepBuffer = mutableListOf<Int>()
    private val heartRateBuffer = mutableListOf<Float>()
    private var isBufferingHeartRate = false
    private val stepMutex = Mutex()
    private val heartRateMutex = Mutex()
    private var isBufferingSteps = false
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
        stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        acquireWakeLock()
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
        releaseWakeLock()
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
                    val savedLastSensorSteps = getLastSensorSteps()

                    if (savedInitialSteps == null || savedDate != currentDate) {
                        initialSteps = currentSteps
                        initialStepsDate = currentDate
                        saveInitialSteps(initialSteps!!, initialStepsDate!!)
                        totalStepsForDay = 0
                        saveLastSensorSteps(currentSteps)
                    } else {
                        initialSteps = savedInitialSteps
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

            if (!isBufferingHeartRate) {
                isBufferingHeartRate = true
                delay(5000)

                val averageHeartRate: Float?
                heartRateMutex.withLock {
                    averageHeartRate = if (heartRateBuffer.isNotEmpty()) {
                        heartRateBuffer.average().toFloat().also { heartRateBuffer.clear() }
                    } else {
                        null
                    }
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
                delay(5000)

                val totalBufferedSteps: Int
                stepMutex.withLock {
                    totalBufferedSteps = stepBuffer.sum()
                    stepBuffer.clear()
                }

                if (totalBufferedSteps > 0) {
                    saveStepsToDatabase(totalBufferedSteps)
                }
                isBufferingSteps = false
            }
        }
    }

    private fun saveStepsToDatabase(steps: Int) {
        val currentTime = getCurrentTime()
        serviceScope.launch {
            try {
                val newEntry = FitnessEntity(
                    date = currentTime,
                    steps = steps,
                    heartRate = null,
                    isSynced = false
                )
                fitnessDao.insertOrUpdateEntry(newEntry)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun saveHeartRateToDatabase(heartRate: Float) {
        val currentTime = getCurrentTime()
        serviceScope.launch {
            try {
                val roundedHeartRate = Math.round(heartRate).toFloat()
                val lastHeartRate = fitnessDao.getLastRecordedHeartRate()

                if (lastHeartRate == null || lastHeartRate != roundedHeartRate) {
                    val newEntry = FitnessEntity(
                        date = currentTime,
                        steps = null,
                        heartRate = roundedHeartRate,
                        isSynced = false
                    )
                    fitnessDao.insertOrUpdateEntry(newEntry)
                } else {
                    // If the last pulse is the same, we do not add a new record
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

    private fun saveLastSensorSteps(steps: Int) {
        val prefs = getSharedPreferences("fitness_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("last_sensor_steps", steps).apply()
    }

    private fun getLastSensorSteps(): Int? {
        val prefs = getSharedPreferences("fitness_prefs", Context.MODE_PRIVATE)
        val steps = prefs.getInt("last_sensor_steps", -1)
        return if (steps != -1) steps else null
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SafeFitness:WakeLock")
        wakeLock?.acquire()
    }

    private fun releaseWakeLock() {
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
    }

}
