package com.archimedeprojects.arihna.feature.alarms.platform

import android.app.KeyguardManager
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import java.time.LocalTime
import java.time.format.DateTimeFormatter

internal interface AlarmOverlayWindowHost {
    fun add(view: View, params: WindowManager.LayoutParams)
    fun remove(view: View)
}

private class AndroidAlarmOverlayWindowHost(
    private val windowManager: WindowManager,
) : AlarmOverlayWindowHost {
    override fun add(view: View, params: WindowManager.LayoutParams) {
        windowManager.addView(view, params)
    }

    override fun remove(view: View) {
        windowManager.removeViewImmediate(view)
    }
}

internal class AlarmRingingOverlay(
    private val context: Context,
    private val canDrawOverlays: () -> Boolean = { AlarmOverlayAccess(context).isGranted() },
    private val isDeviceLocked: () -> Boolean = {
        (context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).isKeyguardLocked
    },
    private val windowHost: AlarmOverlayWindowHost = AndroidAlarmOverlayWindowHost(
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager,
    ),
) {
    private var rootView: View? = null

    internal val isShowing: Boolean
        get() = rootView != null

    fun show(
        payload: AlarmRingingPayload,
        onStop: () -> Unit,
        onSnooze: () -> Unit,
    ): Boolean {
        hide()
        if (!canDrawOverlays() || isDeviceLocked()) return false

        val root = buildView(payload, onStop, onSnooze)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = dp(18)
        }
        return runCatching {
            windowHost.add(root, params)
            rootView = root
            true
        }.getOrElse {
            rootView = null
            false
        }
    }

    fun hide() {
        val current = rootView ?: return
        rootView = null
        runCatching { windowHost.remove(current) }
    }

    private fun buildView(
        payload: AlarmRingingPayload,
        onStop: () -> Unit,
        onSnooze: () -> Unit,
    ): View {
        val root = FrameLayout(context).apply {
            contentDescription = "arihna-alarm-overlay"
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }

        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(18), dp(22), dp(18))
            elevation = dp(18).toFloat()
            background = roundedDrawable(
                fill = ARIHNA_GREEN,
                stroke = ARIHNA_GOLD,
                radiusDp = 28,
                strokeDp = 2,
            )
        }
        root.addView(
            card,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                leftMargin = dp(4)
                rightMargin = dp(4)
            },
        )

        card.addView(TextView(context).apply {
            text = "SVEGLIA ARIHNA"
            setTextColor(ARIHNA_GOLD)
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.12f
        })

        val headline = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, dp(16))
        }
        headline.addView(TextView(context).apply {
            text = payload.title
            setTextColor(Color.WHITE)
            textSize = 25f
            typeface = Typeface.DEFAULT_BOLD
            maxLines = 1
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        headline.addView(TextView(context).apply {
            text = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
            setTextColor(ARIHNA_LIGHT)
            textSize = 25f
            typeface = Typeface.DEFAULT_BOLD
        })
        card.addView(headline)

        card.addView(TextView(context).apply {
            text = when (payload.soundProfile) {
                com.archimedeprojects.arihna.feature.alarms.domain.AlarmSoundProfile.ADHAN -> "Adhan in corso"
                com.archimedeprojects.arihna.feature.alarms.domain.AlarmSoundProfile.SYSTEM_DEFAULT ->
                    payload.ringtoneTitle ?: "Sveglia in corso"
                com.archimedeprojects.arihna.feature.alarms.domain.AlarmSoundProfile.SILENT -> "Sveglia in corso"
            }
            setTextColor(Color.rgb(225, 235, 229))
            textSize = 15f
            setPadding(0, 0, 0, dp(18))
        })

        val actions = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        val stopControl = actionControl(
            text = "Interrompi",
            tagValue = STOP_TAG,
            filled = true,
            onClick = onStop,
        )
        val snoozeControl = actionControl(
            text = "Rinvia 5 min",
            tagValue = SNOOZE_TAG,
            filled = false,
            onClick = onSnooze,
        )
        actions.addView(
            stopControl,
            LinearLayout.LayoutParams(0, dp(58), 1f).apply { rightMargin = dp(7) },
        )
        actions.addView(
            snoozeControl,
            LinearLayout.LayoutParams(0, dp(58), 1f).apply { leftMargin = dp(7) },
        )
        card.addView(actions)
        return root
    }

    private fun actionControl(
        text: String,
        tagValue: String,
        filled: Boolean,
        onClick: () -> Unit,
    ): TextView = TextView(context).apply {
        this.text = text
        tag = tagValue
        gravity = Gravity.CENTER
        textSize = 17f
        typeface = Typeface.DEFAULT_BOLD
        minimumHeight = dp(56)
        setPadding(dp(12), 0, dp(12), 0)
        setTextColor(if (filled) ARIHNA_GREEN else ARIHNA_LIGHT)
        background = roundedDrawable(
            fill = if (filled) ARIHNA_GOLD else ARIHNA_GREEN_DARK,
            stroke = ARIHNA_GOLD,
            radiusDp = 18,
            strokeDp = if (filled) 0 else 2,
        )
        isClickable = true
        isFocusable = true
        contentDescription = text
        setOnClickListener { onClick() }
    }

    private fun roundedDrawable(
        fill: Int,
        stroke: Int,
        radiusDp: Int,
        strokeDp: Int,
    ): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        setColor(fill)
        cornerRadius = dp(radiusDp).toFloat()
        if (strokeDp > 0) setStroke(dp(strokeDp), stroke)
    }

    private fun dp(value: Int): Int =
        (value * context.resources.displayMetrics.density).toInt()

    companion object {
        internal const val STOP_TAG = "arihna-overlay-stop"
        internal const val SNOOZE_TAG = "arihna-overlay-snooze"
        internal val ARIHNA_GREEN: Int = Color.rgb(8, 58, 39)
        internal val ARIHNA_GREEN_DARK: Int = Color.rgb(12, 73, 48)
        internal val ARIHNA_GOLD: Int = Color.rgb(212, 175, 55)
        internal val ARIHNA_LIGHT: Int = Color.rgb(250, 250, 246)
    }
}
