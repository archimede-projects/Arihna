package com.archimedeprojects.arihna.core.qibla.heading

import com.archimedeprojects.arihna.core.prayer.model.Coordinates
import com.archimedeprojects.arihna.core.qibla.calculation.normalizeDegrees
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow

interface DeviceHeadingDataSource {
    fun observeHeading(origin: Coordinates): Flow<DeviceHeadingState>
}

fun interface MagneticDeclinationProvider {
    fun declinationDegrees(origin: Coordinates): Double
}

class DefaultDeviceHeadingDataSource(
    private val backend: HeadingSensorBackend,
    private val magneticDeclinationProvider: MagneticDeclinationProvider,
) : DeviceHeadingDataSource {
    override fun observeHeading(origin: Coordinates): Flow<DeviceHeadingState> = flow {
        if (!origin.isValid) {
            emit(DeviceHeadingState.Unavailable(HeadingUnavailableReason.INVALID_COORDINATES))
            return@flow
        }

        val source = selectHeadingSource(backend.availableSources())
        if (source == null) {
            emit(DeviceHeadingState.Unavailable(HeadingUnavailableReason.NO_SUPPORTED_SENSOR))
            return@flow
        }

        val declinationDegrees = if (source == HeadingSource.TRUE_HEADING_SENSOR) {
            null
        } else {
            val value = magneticDeclinationProvider.declinationDegrees(origin)
            if (!value.isFinite()) {
                emit(DeviceHeadingState.Unavailable(HeadingUnavailableReason.INVALID_DECLINATION))
                return@flow
            }
            value
        }

        backend.observe(source).collect { event ->
            when (event) {
                HeadingSensorEvent.RegistrationFailed -> {
                    emit(DeviceHeadingState.Unavailable(HeadingUnavailableReason.REGISTRATION_FAILED))
                }

                is HeadingSensorEvent.Reading -> {
                    val rawHeading = event.headingDegrees
                    if (!rawHeading.isFinite()) {
                        emit(DeviceHeadingState.Unavailable(HeadingUnavailableReason.INVALID_SENSOR_READING))
                        return@collect
                    }
                    val estimatedAccuracy = event.estimatedAccuracyDegrees
                        ?.takeIf { it.isFinite() && it >= 0.0 }

                    if (source == HeadingSource.TRUE_HEADING_SENSOR) {
                        emit(
                            DeviceHeadingState.Reading(
                                trueHeadingDegrees = normalizeDegrees(rawHeading),
                                quality = event.quality,
                                estimatedAccuracyDegrees = estimatedAccuracy,
                                source = source,
                                magneticHeadingDegrees = null,
                                declinationDegrees = null,
                            ),
                        )
                    } else {
                        val magneticHeading = normalizeDegrees(rawHeading)
                        emit(
                            DeviceHeadingState.Reading(
                                trueHeadingDegrees = trueHeadingFromMagneticDegrees(
                                    magneticHeadingDegrees = magneticHeading,
                                    declinationDegrees = requireNotNull(declinationDegrees),
                                ),
                                quality = event.quality,
                                estimatedAccuracyDegrees = estimatedAccuracy,
                                source = source,
                                magneticHeadingDegrees = magneticHeading,
                                declinationDegrees = declinationDegrees,
                            ),
                        )
                    }
                }
            }
        }
    }
}
