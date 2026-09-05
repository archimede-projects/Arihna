package com.archimedeprojects.arihna.feature.alarms.platform

import android.app.KeyguardManager
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
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
                fill = Color.rgb(8, 58, 39),
                stroke = Color.rgb(212, 175, 55),
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
            setTextColor(Color.rgb(212, 175, 55))
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
            setTextColor(Color.rgb(250, 250, 246))
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
        val stopButton = actionButton(
            text = "Interrompi",
            filled = true,
            onClick = onStop,
        )
        val snoozeButton = actionButton(
            text = "Rinvia 5 min",
            filled = false,
            onClick = onSnooze,
        )
        actions.addView(
            stopButton,
            LinearLayout.LayoutParams(0, dp(58), 1f).apply { rightMargin = dp(7) },
        )
        actions.addView(
            snoozeButton,
            LinearLayout.LayoutParams(0, dp(58), 1f).apply { leftMargin = dp(7) },
        )
        card.addView(actions)
        return root
    }

    private fun actionButton(
        text: String,
        filled: Boolean,
        onClick: () -> Unit,
    ): Button = Button(context).apply {
        this.text = text
        isAllCaps = false
        textSize = 17f
        typeface = Typeface.DEFAULT_BOLD
        minimumHeight = dp(56)
        setTextColor(if (filled) Color.rgb(8, 58, 39) else Color.rgb(250, 250, 246))
        backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
        background = roundedDrawable(
            fill = if (filled) Color.rgb(212, 175, 55) else Color.rgb(12, 73, 48),
            stroke = Color.rgb(212, 175, 55),
            radiusDp = 18,
            strokeDp = if (filled) 0 else 1,
        )
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
}
