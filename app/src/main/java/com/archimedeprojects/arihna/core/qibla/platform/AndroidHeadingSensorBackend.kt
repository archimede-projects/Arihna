package com.archimedeprojects.arihna.core.qibla.platform

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.display.DisplayManager
import android.os.Build
import android.view.Display
import android.view.Surface
import com.archimedeprojects.arihna.core.qibla.calculation.normalizeDegrees
import com.archimedeprojects.arihna.core.qibla.heading.HeadingQuality
import com.archimedeprojects.arihna.core.qibla.heading.HeadingSensorBackend
import com.archimedeprojects.arihna.core.qibla.heading.HeadingSensorEvent
import com.archimedeprojects.arihna.core.qibla.heading.HeadingSource
import com.archimedeprojects.arihna.core.qibla.heading.combineHeadingQuality
import com.archimedeprojects.arihna.core.qibla.heading.headingForDisplayQuarterTurns
import com.archimedeprojects.arihna.core.qibla.heading.headingQualityFromPlatformAccuracy
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.PI
import kotlin.math.sqrt

class AndroidHeadingSensorBackend(context: Context) : HeadingSensorBackend {
    private val appContext = context.applicationContext
    private val sensorManager = appContext.getSystemService(SensorManager::class.java)
    private val displayManager = appContext.getSystemService(DisplayManager::class.java)

    override fun availableSources(): Set<HeadingSource> = buildSet {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && sensorManager.getDefaultSensor(Sensor.TYPE_HEADING) != null) add(HeadingSource.TRUE_HEADING_SENSOR)
        if (sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null) add(HeadingSource.ROTATION_VECTOR)
        if (sensorManager.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR) != null) add(HeadingSource.GEOMAGNETIC_ROTATION_VECTOR)
        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null && sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null) add(HeadingSource.ACCELEROMETER_MAGNETIC_FIELD)
    }

    override fun observe(source: HeadingSource): Flow<HeadingSensorEvent> = callbackFlow {
        val registration = sensorsFor(source)
        if (registration == null) {
            trySend(HeadingSensorEvent.RegistrationFailed)
            close()
            return@callbackFlow
        }

        var accelerometerValues: FloatArray? = null
        var magneticFieldValues: FloatArray? = null
        var magneticFieldMicroTesla: Double? = null
        var accelerometerQuality = HeadingQuality.UNKNOWN
        var magneticFieldQuality = HeadingQuality.UNKNOWN

        fun updateMagnetic(event: SensorEvent): Boolean {
            if (event.sensor.type != Sensor.TYPE_MAGNETIC_FIELD || event.values.size < 3) return false
            magneticFieldValues = event.values.copyOf(3)
            magneticFieldQuality = headingQualityFromPlatformAccuracy(event.accuracy)
            val x = event.values[0].toDouble()
            val y = event.values[1].toDouble()
            val z = event.values[2].toDouble()
            magneticFieldMicroTesla = sqrt(x * x + y * y + z * z).takeIf { it.isFinite() }
            return true
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val isMagneticEvent = updateMagnetic(event)
                when (source) {
                    HeadingSource.TRUE_HEADING_SENSOR -> {
                        if (isMagneticEvent || event.sensor.type != Sensor.TYPE_HEADING || event.values.isEmpty()) return
                        val heading = headingForDisplayQuarterTurns(event.values[0].toDouble(), displayQuarterTurns())
                        val estimatedAccuracy = event.values.getOrNull(1)?.toDouble()?.takeIf { it.isFinite() && it >= 0.0 }
                        trySend(HeadingSensorEvent.Reading(heading, headingQualityFromPlatformAccuracy(event.accuracy), estimatedAccuracy, magneticFieldMicroTesla))
                    }
                    HeadingSource.ROTATION_VECTOR, HeadingSource.GEOMAGNETIC_ROTATION_VECTOR -> {
                        val expectedType = if (source == HeadingSource.ROTATION_VECTOR) Sensor.TYPE_ROTATION_VECTOR else Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR
                        if (isMagneticEvent || event.sensor.type != expectedType) return
                        val heading = rotationVectorHeadingDegrees(event.values) ?: return
                        val estimatedAccuracy = if (source == HeadingSource.ROTATION_VECTOR) event.values.getOrNull(4)?.toDouble()?.takeIf { it.isFinite() && it >= 0.0 }?.let { it * 180.0 / PI } else null
                        trySend(HeadingSensorEvent.Reading(heading, headingQualityFromPlatformAccuracy(event.accuracy), estimatedAccuracy, magneticFieldMicroTesla))
                    }
                    HeadingSource.ACCELEROMETER_MAGNETIC_FIELD -> {
                        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                            accelerometerValues = event.values.copyOf(3)
                            accelerometerQuality = headingQualityFromPlatformAccuracy(event.accuracy)
                        }
                        val acceleration = accelerometerValues ?: return
                        val magnetic = magneticFieldValues ?: return
                        val heading = accelerometerMagneticHeadingDegrees(acceleration, magnetic) ?: return
                        trySend(HeadingSensorEvent.Reading(heading, combineHeadingQuality(accelerometerQuality, magneticFieldQuality), null, magneticFieldMicroTesla))
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        val registered = registration.all { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
        if (!registered) {
            sensorManager.unregisterListener(listener)
            trySend(HeadingSensorEvent.RegistrationFailed)
            close()
            return@callbackFlow
        }
        awaitClose { sensorManager.unregisterListener(listener) }
    }

    private fun sensorsFor(source: HeadingSource): List<Sensor>? {
        val magnetic = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        fun withMagnetic(primary: Sensor?): List<Sensor>? = primary?.let { p -> if (magnetic != null && magnetic != p) listOf(p, magnetic) else listOf(p) }
        return when (source) {
            HeadingSource.TRUE_HEADING_SENSOR -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) withMagnetic(sensorManager.getDefaultSensor(Sensor.TYPE_HEADING)) else null
            HeadingSource.ROTATION_VECTOR -> withMagnetic(sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR))
            HeadingSource.GEOMAGNETIC_ROTATION_VECTOR -> withMagnetic(sensorManager.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR))
            HeadingSource.ACCELEROMETER_MAGNETIC_FIELD -> {
                val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
                if (accelerometer != null && magnetic != null) listOf(accelerometer, magnetic) else null
            }
        }
    }

    private fun rotationVectorHeadingDegrees(values: FloatArray): Double? {
        if (values.size < 3) return null
        val rotationMatrix = FloatArray(9)
        SensorManager.getRotationMatrixFromVector(rotationMatrix, values)
        return azimuthFromRotationMatrix(rotationMatrix)
    }

    private fun accelerometerMagneticHeadingDegrees(acceleration: FloatArray, magnetic: FloatArray): Double? {
        val rotationMatrix = FloatArray(9)
        if (!SensorManager.getRotationMatrix(rotationMatrix, null, acceleration, magnetic)) return null
        return azimuthFromRotationMatrix(rotationMatrix)
    }

    private fun azimuthFromRotationMatrix(rotationMatrix: FloatArray): Double? {
        val remapped = FloatArray(9)
        val (axisX, axisY) = axesForDisplayRotation(currentDisplayRotation())
        if (!SensorManager.remapCoordinateSystem(rotationMatrix, axisX, axisY, remapped)) return null
        val orientation = FloatArray(3)
        SensorManager.getOrientation(remapped, orientation)
        return normalizeDegrees(orientation[0].toDouble() * 180.0 / PI)
    }

    private fun currentDisplayRotation(): Int = displayManager.getDisplay(Display.DEFAULT_DISPLAY)?.rotation ?: Surface.ROTATION_0
    private fun displayQuarterTurns(): Int = when (currentDisplayRotation()) {
        Surface.ROTATION_0 -> 0
        Surface.ROTATION_90 -> 1
        Surface.ROTATION_180 -> 2
        Surface.ROTATION_270 -> 3
        else -> 0
    }
    private fun axesForDisplayRotation(rotation: Int): Pair<Int, Int> = when (rotation) {
        Surface.ROTATION_0 -> SensorManager.AXIS_X to SensorManager.AXIS_Y
        Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
        Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
        Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
        else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
    }
}
