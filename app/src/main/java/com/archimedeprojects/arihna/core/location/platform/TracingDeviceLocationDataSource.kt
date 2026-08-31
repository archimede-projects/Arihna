package com.archimedeprojects.arihna.core.location.platform

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.archimedeprojects.arihna.core.location.data.DeviceLocationDataSource
import com.archimedeprojects.arihna.core.location.diagnostics.LocationDiagnosticTrace
import com.archimedeprojects.arihna.core.location.model.DeviceLocationFix
import com.archimedeprojects.arihna.core.location.model.DeviceLocationResult
import java.time.Clock
import java.time.Duration
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart

class TracingDeviceLocationDataSource(
    context: Context,
    private val delegate: DeviceLocationDataSource,
    private val clock: Clock = Clock.systemUTC(),
) : DeviceLocationDataSource {
    private val appContext = context.applicationContext
    private val locationManager = appContext.getSystemService(LocationManager::class.java)

    override suspend fun getCurrentLocation(): DeviceLocationResult {
        val coarseGranted = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val locationEnabled = runCatching {
            LocationManagerCompat.isLocationEnabled(locationManager)
        }.getOrDefault(false)
        val enabledProviders = runCatching {
            locationManager.getProviders(true).joinToString(prefix = "[", postfix = "]")
        }.getOrDefault("<provider-query-failed>")

        LocationDiagnosticTrace.record(
            stage = "DATASOURCE_ENTER",
            detail = "coarse=$coarseGranted locationEnabled=$locationEnabled enabledProviders=$enabledProviders",
        )

        return try {
            delegate.getCurrentLocation().also { result ->
                when (result) {
                    is DeviceLocationResult.Success -> LocationDiagnosticTrace.record(
                        stage = "DATASOURCE_RETURN_SUCCESS",
                        detail = fixDetail(result.fix),
                    )
                    is DeviceLocationResult.Unavailable -> LocationDiagnosticTrace.record(
                        stage = "DATASOURCE_RETURN_UNAVAILABLE",
                        detail = "reason=${result.reason}",
                    )
                }
            }
        } catch (error: CancellationException) {
            LocationDiagnosticTrace.record(
                stage = "DATASOURCE_CANCELLED",
                detail = error.javaClass.simpleName,
            )
            throw error
        } catch (error: Throwable) {
            LocationDiagnosticTrace.record(
                stage = "DATASOURCE_THROW",
                detail = "${error.javaClass.simpleName}: ${error.message}",
            )
            throw error
        }
    }

    override fun observeSignificantUpdates(): Flow<DeviceLocationFix> =
        delegate.observeSignificantUpdates()
            .onStart { LocationDiagnosticTrace.record("UPDATES_SUBSCRIBE") }
            .onEach { fix ->
                LocationDiagnosticTrace.record(
                    stage = "UPDATES_FIX",
                    detail = fixDetail(fix),
                )
            }
            .onCompletion { cause ->
                LocationDiagnosticTrace.record(
                    stage = "UPDATES_COMPLETE",
                    detail = cause?.let { "${it.javaClass.simpleName}: ${it.message}" } ?: "normal",
                )
            }

    private fun fixDetail(fix: DeviceLocationFix): String {
        val ageMs = Duration.between(fix.capturedAt, clock.instant()).toMillis().coerceAtLeast(0L)
        return "valid=${fix.isValid} ageMs=$ageMs accuracy=${fix.accuracyMeters} zone=${fix.zoneId.id} capturedAt=${fix.capturedAt}"
    }
}

internal fun tracingCoarseProviderSelector(locationManager: LocationManager): String? {
    val provider = selectCoarseProvider(locationManager)
    val enabledProviders = runCatching {
        locationManager.getProviders(true).joinToString(prefix = "[", postfix = "]")
    }.getOrDefault("<provider-query-failed>")
    LocationDiagnosticTrace.record(
        stage = "PROVIDER_SELECTED",
        detail = "provider=${provider ?: "<none>"} enabledProviders=$enabledProviders",
    )
    return provider
}
