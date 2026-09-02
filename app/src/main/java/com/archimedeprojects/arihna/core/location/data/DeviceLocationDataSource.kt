package com.archimedeprojects.arihna.core.location.data

import com.archimedeprojects.arihna.core.location.model.DeviceLocationFix
import com.archimedeprojects.arihna.core.location.model.DeviceLocationResult
import kotlinx.coroutines.flow.Flow

interface DeviceLocationDataSource {
    suspend fun getCurrentLocation(): DeviceLocationResult
    suspend fun getLastKnownLocation(): DeviceLocationResult
    fun observeSignificantUpdates(): Flow<DeviceLocationFix>
}
