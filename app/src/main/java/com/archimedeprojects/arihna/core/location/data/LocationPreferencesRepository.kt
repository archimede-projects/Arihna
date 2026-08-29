package com.archimedeprojects.arihna.core.location.data

import com.archimedeprojects.arihna.core.location.model.DeviceLocationFix
import com.archimedeprojects.arihna.core.location.model.LocationPreference
import com.archimedeprojects.arihna.core.location.model.ManualCity
import kotlinx.coroutines.flow.Flow

interface LocationPreferencesRepository {
    val preference: Flow<LocationPreference>
    val cachedDeviceFix: Flow<DeviceLocationFix?>

    suspend fun selectDevice()
    suspend fun selectManual(city: ManualCity)
    suspend fun saveDeviceFix(fix: DeviceLocationFix)
}
