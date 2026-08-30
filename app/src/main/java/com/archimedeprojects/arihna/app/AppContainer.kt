package com.archimedeprojects.arihna.app

import android.content.Context
import com.archimedeprojects.arihna.core.location.data.CityRepository
import com.archimedeprojects.arihna.core.location.data.DeviceLocationDataSource
import com.archimedeprojects.arihna.core.location.data.sqlite.SQLiteCityRepository
import com.archimedeprojects.arihna.core.location.platform.AndroidLocationEnvironment
import com.archimedeprojects.arihna.core.location.platform.AndroidLocationPermissionStateResolver
import com.archimedeprojects.arihna.core.location.platform.LocationManagerDeviceLocationDataSource

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val cityRepository: CityRepository by lazy {
        SQLiteCityRepository(appContext)
    }

    val deviceLocationDataSource: DeviceLocationDataSource by lazy {
        LocationManagerDeviceLocationDataSource(appContext)
    }

    val locationEnvironment: AndroidLocationEnvironment by lazy {
        AndroidLocationEnvironment(appContext)
    }

    val locationPermissionStateResolver: AndroidLocationPermissionStateResolver by lazy {
        AndroidLocationPermissionStateResolver(appContext)
    }
}
