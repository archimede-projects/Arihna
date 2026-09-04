package com.archimedeprojects.arihna.core.qibla.heading

enum class HeadingSource {
    TRUE_HEADING_SENSOR,
    ROTATION_VECTOR,
    GEOMAGNETIC_ROTATION_VECTOR,
    ACCELEROMETER_MAGNETIC_FIELD,
}

enum class HeadingQuality {
    HIGH,
    MEDIUM,
    LOW,
    UNRELIABLE,
    UNKNOWN,
}

enum class HeadingUnavailableReason {
    NO_SUPPORTED_SENSOR,
    REGISTRATION_FAILED,
    INVALID_COORDINATES,
    INVALID_SENSOR_READING,
    INVALID_DECLINATION,
}

sealed interface DeviceHeadingState {
    data class Unavailable(val reason: HeadingUnavailableReason) : DeviceHeadingState

    data class Reading(
        val trueHeadingDegrees: Double,
        val quality: HeadingQuality,
        val estimatedAccuracyDegrees: Double?,
        val source: HeadingSource,
        val magneticHeadingDegrees: Double?,
        val declinationDegrees: Double?,
        val magneticFieldMicroTesla: Double? = null,
    ) : DeviceHeadingState
}
