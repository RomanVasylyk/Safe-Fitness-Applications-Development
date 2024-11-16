package com.example.safefitness

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class SensorManagerHelper(private val context: Context) : SensorEventListener {
    private val sensorManager: SensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val fitnessDatabaseHelper = FitnessDatabaseHelper(context)

    private var heartRateSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
    private var stepCounterSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    var onHeartRateChanged: ((Float) -> Unit)? = null
    var onStepCountChanged: ((Int) -> Unit)? = null

    private var initialSteps: Int? = null
    private var totalStepsForDay = 0

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
                    fitnessDatabaseHelper.insertData(null, heartRate)
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
                        fitnessDatabaseHelper.insertData(addedSteps, null)
                        totalStepsForDay += addedSteps
                    }

                    onStepCountChanged?.invoke(totalStepsForDay)
                }
                else -> {}
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
