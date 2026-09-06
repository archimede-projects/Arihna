package com.archimedeprojects.arihna.core.location.platform

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.archimedeprojects.arihna.core.location.model.LocationPermissionState

/** Android-owned capability state. This class never opens a permission dialog by itself. */
class AndroidLocationEnvironment(context: Context) {
    private val appContext = context.applicationContext
    private val locationManager = appContext.getSystemService(LocationManager::class.java)

    fun isLocationPermissionGranted(): Boolean =
        ContextCompat.checkSelfPermission(appContext, FINE_LOCATION_PERMISSION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(appContext, COARSE_LOCATION_PERMISSION) ==
            PackageManager.PERMISSION_GRANTED

    /** Compatibility alias for existing callers/tests; either foreground precision is sufficient. */
    fun isCoarsePermissionGranted(): Boolean = isLocationPermissionGranted()

    fun isLocationServicesEnabled(): Boolean = LocationManagerCompat.isLocationEnabled(locationManager)
}

/** Converts Android foreground location permission signals into Arihna domain state. */
class AndroidLocationPermissionStateResolver(private val context: Context) {
    val permissions: Array<String> = arrayOf(FINE_LOCATION_PERMISSION, COARSE_LOCATION_PERMISSION)

    /** Kept for source compatibility; new request flows launch [permissions] together. */
    val permission: String = COARSE_LOCATION_PERMISSION

    fun resolve(activity: Activity, hasRequestedBefore: Boolean): LocationPermissionState {
        if (hasForegroundPermission()) return LocationPermissionState.Granted
        if (!hasRequestedBefore) return LocationPermissionState.NotRequested

        return LocationPermissionState.Denied(
            canRequestAgain =
                ActivityCompat.shouldShowRequestPermissionRationale(activity, FINE_LOCATION_PERMISSION) ||
                ActivityCompat.shouldShowRequestPermissionRationale(activity, COARSE_LOCATION_PERMISSION),
        )
    }

    private fun hasForegroundPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, FINE_LOCATION_PERMISSION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, COARSE_LOCATION_PERMISSION) ==
            PackageManager.PERMISSION_GRANTED
}

const val FINE_LOCATION_PERMISSION: String = Manifest.permission.ACCESS_FINE_LOCATION
const val COARSE_LOCATION_PERMISSION: String = Manifest.permission.ACCESS_COARSE_LOCATION
