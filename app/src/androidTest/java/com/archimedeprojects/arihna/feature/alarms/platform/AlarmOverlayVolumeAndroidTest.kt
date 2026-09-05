package com.archimedeprojects.arihna.feature.alarms.platform

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmSoundProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AlarmOverlayVolumeAndroidTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun manifestDeclaresOverlaySpecialAccessAndIntentMatchesPlatform() {
        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS,
        )
        assertTrue(packageInfo.requestedPermissions.orEmpty().contains(Manifest.permission.SYSTEM_ALERT_WINDOW))

        val access = AlarmOverlayAccess(context)
        assertEquals(Settings.canDrawOverlays(context), access.isGranted())
        val intent = access.createSettingsIntent()
        assertEquals(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, intent.action)
        assertEquals("package", intent.data?.scheme)
        assertEquals(context.packageName, intent.data?.schemeSpecificPart)
    }

    @Test
    fun overlayRequiresAuthorizationAndUnlockedStateAndIsRemoved() {
        val deniedHost = FakeWindowHost()
        val denied = AlarmRingingOverlay(
            context = context,
            canDrawOverlays = { false },
            isDeviceLocked = { false },
            windowHost = deniedHost,
        )
        assertFalse(denied.show(payload(), {}, {}))
        assertFalse(denied.isShowing)
        assertEquals(0, deniedHost.addCount)

        val lockedHost = FakeWindowHost()
        val locked = AlarmRingingOverlay(
            context = context,
            canDrawOverlays = { true },
            isDeviceLocked = { true },
            windowHost = lockedHost,
        )
        assertFalse(locked.show(payload(), {}, {}))
        assertFalse(locked.isShowing)
        assertEquals(0, lockedHost.addCount)

        val allowedHost = FakeWindowHost()
        val allowed = AlarmRingingOverlay(
            context = context,
            canDrawOverlays = { true },
            isDeviceLocked = { false },
            windowHost = allowedHost,
        )
        assertTrue(allowed.show(payload(), {}, {}))
        assertTrue(allowed.isShowing)
        assertEquals(1, allowedHost.addCount)
        assertNotNull(allowedHost.lastView)
        allowed.hide()
        assertFalse(allowed.isShowing)
        assertEquals(1, allowedHost.removeCount)
    }

    @Test
    fun overlayActionsUseThemeIndependentHighContrastTextControls() {
        val host = FakeWindowHost()
        var stopClicks = 0
        var snoozeClicks = 0
        val overlay = AlarmRingingOverlay(
            context = context,
            canDrawOverlays = { true },
            isDeviceLocked = { false },
            windowHost = host,
        )

        assertTrue(
            overlay.show(
                payload = payload(),
                onStop = { stopClicks += 1 },
                onSnooze = { snoozeClicks += 1 },
            ),
        )

        val root = host.lastView ?: error("overlay root missing")
        assertFalse(containsNativeButton(root))

        val stop = root.findViewWithTag<TextView>(AlarmRingingOverlay.STOP_TAG)
        val snooze = root.findViewWithTag<TextView>(AlarmRingingOverlay.SNOOZE_TAG)
        assertNotNull(stop)
        assertNotNull(snooze)
        assertEquals("Interrompi", stop.text.toString())
        assertEquals("Rinvia 5 min", snooze.text.toString())
        assertTrue(stop.isClickable)
        assertTrue(snooze.isClickable)
        assertEquals(AlarmRingingOverlay.ARIHNA_GREEN, stop.currentTextColor)
        assertEquals(AlarmRingingOverlay.ARIHNA_LIGHT, snooze.currentTextColor)
        assertEquals(
            AlarmRingingOverlay.ARIHNA_GOLD,
            (stop.background as GradientDrawable).color?.defaultColor,
        )
        assertEquals(
            AlarmRingingOverlay.ARIHNA_GREEN_DARK,
            (snooze.background as GradientDrawable).color?.defaultColor,
        )

        assertTrue(stop.performClick())
        assertTrue(snooze.performClick())
        assertEquals(1, stopClicks)
        assertEquals(1, snoozeClicks)
        overlay.hide()
    }

    @Test
    fun alarmVolumeControllerReadsAndReappliesRealAlarmStream() {
        val controller = AlarmVolumeController(context)
        val before = controller.read()
        assertTrue(before.current in before.min..before.max)
        assertTrue(before.percent in 0..100)

        when (val result = controller.setVolume(before.current)) {
            is AlarmVolumeChangeResult.Success -> {
                assertEquals(before.current, result.state.current)
                assertTrue(result.state.percent in 0..100)
            }
            is AlarmVolumeChangeResult.Failure -> {
                assertTrue(result.message.isNotBlank())
                assertTrue(result.state.current in result.state.min..result.state.max)
            }
        }
    }

    private fun payload() = AlarmRingingPayload(
        alarmId = "overlay-test",
        ruleRevision = 1L,
        title = "Test sveglia",
        soundProfile = AlarmSoundProfile.SYSTEM_DEFAULT,
        isPrayer = false,
        occurrenceToken = "overlay-token",
        ringtoneTitle = "Galaxy Bells",
    )

    private fun containsNativeButton(view: View): Boolean {
        if (view is Button) return true
        if (view !is ViewGroup) return false
        return (0 until view.childCount).any { containsNativeButton(view.getChildAt(it)) }
    }

    private class FakeWindowHost : AlarmOverlayWindowHost {
        var addCount = 0
        var removeCount = 0
        var lastView: View? = null

        override fun add(view: View, params: WindowManager.LayoutParams) {
            addCount += 1
            lastView = view
        }

        override fun remove(view: View) {
            removeCount += 1
            if (lastView === view) lastView = null
        }
    }
}
