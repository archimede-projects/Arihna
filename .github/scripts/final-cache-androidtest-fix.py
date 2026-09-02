from pathlib import Path

path = Path('app/src/androidTest/java/com/archimedeprojects/arihna/core/location/data/preferences/PreferencesDataStoreLocationPreferencesRepositoryAndroidTest.kt')
text = path.read_text()
old = '''    private fun unusedDeviceLocationSource() = object : DeviceLocationDataSource {\n        override suspend fun getCurrentLocation(): DeviceLocationResult = error("Device source must not be called")\n        override fun observeSignificantUpdates(): Flow<DeviceLocationFix> = emptyFlow()\n    }\n'''
new = '''    private fun unusedDeviceLocationSource() = object : DeviceLocationDataSource {\n        override suspend fun getCurrentLocation(): DeviceLocationResult = error("Device source must not be called")\n        override suspend fun getLastKnownLocation(): DeviceLocationResult = error("Device source must not be called")\n        override fun observeSignificantUpdates(): Flow<DeviceLocationFix> = emptyFlow()\n    }\n'''
if old not in text:
    raise SystemExit('androidTest datasource fake block not found')
path.write_text(text.replace(old, new, 1))
