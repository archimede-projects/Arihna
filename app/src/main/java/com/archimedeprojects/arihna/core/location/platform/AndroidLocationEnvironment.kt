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

    fun isCoarsePermissionGranted(): Boolean =
        ContextCompat.checkSelfPermission(appContext, COARSE_LOCATION_PERMISSION) ==
            PackageManager.PERMISSION_GRANTED

    fun isLocationServicesEnabled(): Boolean = LocationManagerCompat.isLocationEnabled(locationManager)
}

/**
 * Converts Android permission signals into Arihna domain state.
 *
 * STEP 6 will call [resolve] from the explicit Device-location action. This class has no
 * requestPermissions/ActivityResult side effect, so constructing it at startup cannot show a prompt.
 */
class AndroidLocationPermissionStateResolver(private val context: Context) {
    val permission: String = COARSE_LOCATION_PERMISSION

    fun resolve(activity: Activity, hasRequestedBefore: Boolean): LocationPermissionState {
        if (
            ContextCompat.checkSelfPermission(context, COARSE_LOCATION_PERMISSION) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            return LocationPermissionState.Granted
        }
        if (!hasRequestedBefore) return LocationPermissionState.NotRequested

        return LocationPermissionState.Denied(
            canRequestAgain = ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                COARSE_LOCATION_PERMISSION,
            ),
        )
    }
}

const val COARSE_LOCATION_PERMISSION: String = Manifest.permission.ACCESS_COARSE_LOCATION
