package com.example.safefitness.helpers

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.example.safefitness.data.FitnessDatabase
import com.example.safefitness.data.FitnessEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class SensorManagerHelper(private val context: Context) : SensorEventListener {
    private val sensorManager: SensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val fitnessDao = FitnessDatabase.getDatabase(context).fitnessDao()

    private var heartRateSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
    private var stepCounterSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    var onHeartRateChanged: ((Float) -> Unit)? = null
    var onStepCountChanged: ((Int) -> Unit)? = null

    private var initialSteps: Int? = null
    private var totalStepsForDay = 0
    private var stepBuffer = 0
    private var isBuffering = false

    fun startListening() {
        heartRateSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        stepCounterSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            when (it.sensor.type) {
                Sensor.TYPE_HEART_RATE -> {
                    val heartRate = it.values[0]
                    saveHeartRateToDatabase(heartRate)
                    onHeartRateChanged?.invoke(heartRate)
                }
                Sensor.TYPE_STEP_COUNTER -> {
                    val currentSteps = it.values[0].toInt()
                    if (initialSteps == null) {
                        initialSteps = currentSteps
                    }

                    val todaySteps = currentSteps - (initialSteps ?: 0)
                    val addedSteps = todaySteps - totalStepsForDay

                    if (addedSteps > 0) {
                        bufferSteps(addedSteps)
                        totalStepsForDay += addedSteps
                    }

                    onStepCountChanged?.invoke(totalStepsForDay)
                }
                else -> {}
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
                fitnessDao.insertData(FitnessEntity(date = currentTime, steps = null, heartRate = heartRate, isSynced = false))
            }
        }
    }


    private fun getCurrentTime(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
