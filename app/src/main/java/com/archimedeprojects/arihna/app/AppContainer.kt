package com.archimedeprojects.arihna.app

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.archimedeprojects.arihna.core.location.data.CityRepository
import com.archimedeprojects.arihna.core.location.data.DeviceLocationDataSource
import com.archimedeprojects.arihna.core.location.data.LocationPreferencesRepository
import com.archimedeprojects.arihna.core.location.data.preferences.PreferencesDataStoreLocationPreferencesRepository
import com.archimedeprojects.arihna.core.location.data.sqlite.SQLiteCityRepository
import com.archimedeprojects.arihna.core.location.domain.LocationCoordinator
import com.archimedeprojects.arihna.core.location.platform.AndroidLocationEnvironment
import com.archimedeprojects.arihna.core.location.platform.AndroidLocationPermissionStateResolver
import com.archimedeprojects.arihna.core.location.platform.LocationManagerDeviceLocationDataSource
import com.archimedeprojects.arihna.core.qibla.calculation.GreatCircleQiblaBearingCalculator
import com.archimedeprojects.arihna.core.qibla.calculation.QiblaBearingCalculator
import com.archimedeprojects.arihna.core.qibla.heading.DeviceHeadingDataSource
import com.archimedeprojects.arihna.core.qibla.platform.AndroidDeviceHeadingDataSource
import com.archimedeprojects.arihna.feature.alarms.data.AlarmRuleRepository
import com.archimedeprojects.arihna.feature.alarms.data.preferences.PreferencesDataStoreAlarmRuleRepository
import com.archimedeprojects.arihna.feature.prayerschedule.data.PrayerSettingsRepository
import com.archimedeprojects.arihna.feature.prayerschedule.data.preferences.PreferencesDataStorePrayerSettingsRepository

private val Context.locationPreferencesDataStore by preferencesDataStore(name = "location")

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val cityRepository: CityRepository by lazy {
        SQLiteCityRepository(appContext)
    }

    val deviceLocationDataSource: DeviceLocationDataSource by lazy {
        LocationManagerDeviceLocationDataSource(appContext)
    }

    val locationPreferencesRepository: LocationPreferencesRepository by lazy {
        PreferencesDataStoreLocationPreferencesRepository(appContext.locationPreferencesDataStore)
    }

    val prayerSettingsRepository: PrayerSettingsRepository by lazy {
        PreferencesDataStorePrayerSettingsRepository(appContext.locationPreferencesDataStore)
    }

    val alarmRuleRepository: AlarmRuleRepository by lazy {
        PreferencesDataStoreAlarmRuleRepository(appContext.locationPreferencesDataStore)
    }

    val locationCoordinator: LocationCoordinator by lazy {
        LocationCoordinator(
            deviceLocationDataSource = deviceLocationDataSource,
            cityRepository = cityRepository,
            preferencesRepository = locationPreferencesRepository,
        )
    }

    val locationEnvironment: AndroidLocationEnvironment by lazy {
        AndroidLocationEnvironment(appContext)
    }

    val locationPermissionStateResolver: AndroidLocationPermissionStateResolver by lazy {
        AndroidLocationPermissionStateResolver(appContext)
    }

    val qiblaBearingCalculator: QiblaBearingCalculator = GreatCircleQiblaBearingCalculator

    val qiblaHeadingDataSource: DeviceHeadingDataSource by lazy {
        AndroidDeviceHeadingDataSource(appContext)
    }
}
