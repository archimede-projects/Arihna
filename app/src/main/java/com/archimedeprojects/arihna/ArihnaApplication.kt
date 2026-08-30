package com.archimedeprojects.arihna

import android.app.Application
import com.archimedeprojects.arihna.app.AppContainer

class ArihnaApplication : Application() {
    val appContainer: AppContainer by lazy { AppContainer(this) }
}
