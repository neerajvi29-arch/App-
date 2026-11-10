package com.example.data.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class StepSensorManager(context: Context) : SensorEventListener {

  private val sensorManager =
    context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
  private val stepCounterSensor: Sensor? =
    sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
  private val stepDetectorSensor: Sensor? =
    sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

  private val _isSensorAvailable = MutableStateFlow(
    stepCounterSensor != null || stepDetectorSensor != null
  )
  val isSensorAvailable: StateFlow<Boolean> = _isSensorAvailable.asStateFlow()

  private var initialHardwareSteps: Long = -1L
  private var onStepIncrementListener: ((Int) -> Unit)? = null

  fun startListening(onStepIncrement: (Int) -> Unit) {
    this.onStepIncrementListener = onStepIncrement
    stepDetectorSensor?.let { sensor ->
      sensorManager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
    } ?: stepCounterSensor?.let { sensor ->
      sensorManager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
    }
  }

  fun stopListening() {
    sensorManager?.unregisterListener(this)
    onStepIncrementListener = null
  }

  override fun onSensorChanged(event: SensorEvent?) {
    if (event == null) return

    when (event.sensor.type) {
      Sensor.TYPE_STEP_DETECTOR -> {
        if (event.values.isNotEmpty() && event.values[0] == 1.0f) {
          onStepIncrementListener?.invoke(1)
        }
      }
      Sensor.TYPE_STEP_COUNTER -> {
        val totalRecorded = event.values[0].toLong()
        if (initialHardwareSteps < 0) {
          initialHardwareSteps = totalRecorded
        } else {
          val diff = (totalRecorded - initialHardwareSteps).toInt()
          if (diff > 0) {
            onStepIncrementListener?.invoke(diff)
            initialHardwareSteps = totalRecorded
          }
        }
      }
    }
  }

  override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    // Sensor accuracy adjustment if required
  }
}
