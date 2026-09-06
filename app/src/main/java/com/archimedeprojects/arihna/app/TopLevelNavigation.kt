package com.archimedeprojects.arihna.app

import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController

internal fun NavHostController.navigateTopLevel(
    route: String,
    homeRoute: String,
) {
    if (route == homeRoute) {
        val poppedToHome = popBackStack(homeRoute, inclusive = false)
        if (!poppedToHome && currentDestination?.route != homeRoute) {
            navigate(homeRoute) {
                popUpTo(graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
        return
    }

    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
