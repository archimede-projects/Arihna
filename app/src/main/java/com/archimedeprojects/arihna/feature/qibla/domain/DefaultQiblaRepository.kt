package com.archimedeprojects.arihna.feature.qibla.domain

import com.archimedeprojects.arihna.core.location.model.LocationResolutionState
import com.archimedeprojects.arihna.core.location.model.LocationSource
import com.archimedeprojects.arihna.core.location.model.SelectedLocation
import com.archimedeprojects.arihna.core.qibla.calculation.QiblaBearingCalculator
import com.archimedeprojects.arihna.core.qibla.calculation.relativeQiblaDirectionDegrees
import com.archimedeprojects.arihna.core.qibla.heading.DeviceHeadingDataSource
import com.archimedeprojects.arihna.core.qibla.heading.DeviceHeadingState
import com.archimedeprojects.arihna.core.qibla.model.QiblaBearingResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultQiblaRepository(
    private val locationStates: Flow<LocationResolutionState>,
    private val bearingCalculator: QiblaBearingCalculator,
    private val headingDataSource: DeviceHeadingDataSource,
) : QiblaRepository {
    override fun observeQibla(): Flow<QiblaState> =
        locationStates
            .distinctUntilChanged()
            .flatMapLatest(::stateForLocation)
            .distinctUntilChanged()

    private fun stateForLocation(locationState: LocationResolutionState): Flow<QiblaState> {
        val ready = locationState as? LocationResolutionState.Ready
            ?: return flowOf(QiblaState.NoLocation(locationState))
        val location = ready.location
        if (!location.isValid) return flowOf(QiblaState.NoLocation(locationState))

        return when (val bearing = bearingCalculator.calculate(location.coordinates)) {
            QiblaBearingResult.InvalidCoordinates -> flowOf(
                QiblaState.BearingUnavailable(location, QiblaBearingUnavailableReason.INVALID_COORDINATES),
            )
            QiblaBearingResult.AtKaabaOrCoincident -> flowOf(
                QiblaState.BearingUnavailable(location, QiblaBearingUnavailableReason.AT_KAABA_OR_COINCIDENT),
            )
            is QiblaBearingResult.Success -> stateForBearing(location, bearing.bearingTrueDegrees)
        }
    }

    private fun stateForBearing(
        location: SelectedLocation,
        bearingTrueDegrees: Double,
    ): Flow<QiblaState> = when (location.source) {
        is LocationSource.Manual -> flowOf(QiblaState.StaticBearing(location, bearingTrueDegrees))
        is LocationSource.Device -> headingDataSource.observeHeading(location.coordinates)
            .map { headingState ->
                when (headingState) {
                    is DeviceHeadingState.Unavailable -> QiblaState.SensorUnavailable(
                        location = location,
                        bearingTrueDegrees = bearingTrueDegrees,
                        reason = headingState.reason,
                    )
                    is DeviceHeadingState.Reading -> QiblaState.LiveCompass(
                        location = location,
                        bearingTrueDegrees = bearingTrueDegrees,
                        deviceHeadingTrueDegrees = headingState.trueHeadingDegrees,
                        relativeQiblaDirectionDegrees = relativeQiblaDirectionDegrees(
                            qiblaBearingTrueDegrees = bearingTrueDegrees,
                            deviceHeadingTrueDegrees = headingState.trueHeadingDegrees,
                        ),
                        quality = headingState.quality,
                        estimatedAccuracyDegrees = headingState.estimatedAccuracyDegrees,
                        headingSource = headingState.source,
                        declinationDegrees = headingState.declinationDegrees,
                        magneticFieldMicroTesla = headingState.magneticFieldMicroTesla,
                    )
                }
            }
            .onStart { emit(QiblaState.LiveCompassStarting(location, bearingTrueDegrees)) }
    }
}
