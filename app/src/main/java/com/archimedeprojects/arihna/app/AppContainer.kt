package com.archimedeprojects.arihna.app

import android.content.Context
import com.archimedeprojects.arihna.core.location.data.CityRepository
import com.archimedeprojects.arihna.core.location.data.sqlite.SQLiteCityRepository

/** Manual dependency container for Arihna. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val cityRepository: CityRepository by lazy {
        SQLiteCityRepository(appContext)
    }
}
