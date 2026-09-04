package com.archimedeprojects.arihna.app

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.archimedeprojects.arihna.core.location.data.CityRepository
import com.archimedeprojects.arihna.core.location.data.DeviceLocationDataSource
import com.archimedeprojects.arihna.core.location.data.LocationPreferencesRepository
import com.archimedeprojects.arihna.core.location.data.preferences.PreferencesDataStoreLocationPreferencesRepository
import com.archimedeprojects.arihna.core.location.data.sqlite.SQLiteCityRepository
import com.archimedeprojects.arihna.core.location.domain.LocationCoordinator
import com.archimedeprojects.arihna.core.location.model.LocationPermissionState
import com.archimedeprojects.arihna.core.location.platform.AndroidLocationEnvironment
import com.archimedeprojects.arihna.core.location.platform.AndroidLocationPermissionStateResolver
import com.archimedeprojects.arihna.core.location.platform.LocationManagerDeviceLocationDataSource
import com.archimedeprojects.arihna.core.prayer.calculation.AdhanPrayerTimeCalculator
import com.archimedeprojects.arihna.core.qibla.calculation.GreatCircleQiblaBearingCalculator
import com.archimedeprojects.arihna.core.qibla.calculation.QiblaBearingCalculator
import com.archimedeprojects.arihna.core.qibla.heading.DeviceHeadingDataSource
import com.archimedeprojects.arihna.core.qibla.platform.AndroidDeviceHeadingDataSource
import com.archimedeprojects.arihna.feature.alarms.data.AlarmRuleRepository
import com.archimedeprojects.arihna.feature.alarms.data.preferences.PreferencesDataStoreAlarmRuleRepository
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmReconciler
import com.archimedeprojects.arihna.feature.alarms.domain.RepositoryAlarmPrayerScheduleSource
import com.archimedeprojects.arihna.feature.alarms.platform.AlarmPlatformScheduler
import com.archimedeprojects.arihna.feature.alarms.platform.AlarmReconciliationTrigger
import com.archimedeprojects.arihna.feature.alarms.platform.AndroidExactAlarmBackend
import com.archimedeprojects.arihna.feature.alarms.platform.DefaultAlarmPlatformScheduler
import com.archimedeprojects.arihna.feature.alarms.platform.DefaultAlarmReconciliationTrigger
import com.archimedeprojects.arihna.feature.alarms.platform.ExactAlarmAccessIntentFactory
import com.archimedeprojects.arihna.feature.prayerschedule.data.PrayerSettingsRepository
import com.archimedeprojects.arihna.feature.prayerschedule.data.preferences.PreferencesDataStorePrayerSettingsRepository
import com.archimedeprojects.arihna.feature.prayerschedule.domain.DefaultPrayerScheduleRepository
import java.time.Clock
import java.time.ZoneId
import kotlinx.coroutines.flow.flow

private val Context.locationPreferencesDataStore by preferencesDataStore(name = "location")

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val alarmClock: Clock = Clock.systemUTC()

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

    val alarmPlatformScheduler: AlarmPlatformScheduler by lazy {
        DefaultAlarmPlatformScheduler(AndroidExactAlarmBackend(appContext))
    }

    val exactAlarmAccessIntentFactory: ExactAlarmAccessIntentFactory by lazy {
        ExactAlarmAccessIntentFactory(appContext)
    }

    private val backgroundPrayerScheduleRepository by lazy {
        DefaultPrayerScheduleRepository(
            locationStates = flow {
                // Background alarm reconciliation must never acquire a new fix. Passing Granted/true
                // here only asks the closed coordinator to expose an already-persisted accepted fix.
                emit(
                    locationCoordinator.restorePersistedState(
                        permissionState = LocationPermissionState.Granted,
                        locationServicesEnabled = true,
                    ),
                )
            },
            prayerSettingsRepository = prayerSettingsRepository,
            prayerTimeCalculator = AdhanPrayerTimeCalculator(),
            clock = alarmClock,
        )
    }

    val alarmReconciler: AlarmReconciler by lazy {
        val alarmPrayerCalculator = AdhanPrayerTimeCalculator()
        AlarmReconciler(
            ruleRepository = alarmRuleRepository,
            platformScheduler = alarmPlatformScheduler,
            prayerScheduleSource = RepositoryAlarmPrayerScheduleSource(
                repository = backgroundPrayerScheduleRepository,
                prayerTimeCalculator = alarmPrayerCalculator,
            ),
            clock = alarmClock,
            deviceZoneId = { ZoneId.systemDefault() },
        )
    }

    val alarmReconciliationTrigger: AlarmReconciliationTrigger by lazy {
        DefaultAlarmReconciliationTrigger(alarmReconciler)
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
