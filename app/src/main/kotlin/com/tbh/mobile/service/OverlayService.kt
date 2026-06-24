package com.tbh.mobile.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.graphics.drawable.Icon
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import com.tbh.mobile.R
import com.tbh.mobile.battle.BattleEngine
import com.tbh.mobile.battle.BattleState
import com.tbh.mobile.overlay.OverlayView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: OverlayView? = null
    private var battleState = BattleState.initial()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        ensureNotificationChannel()
        val notification = buildNotification()
        // Android 14+ wymaga jawnego podania typu usługi.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        attachOverlay()
        startGameLoop()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) stopSelf()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        overlayView?.let { windowManager.removeView(it) }
        overlayView = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ----- Game loop -----

    private fun startGameLoop() {
        scope.launch {
            while (isActive) {
                delay(TICK_MS)
                battleState = BattleEngine.tick(battleState)
                overlayView?.state = battleState   // setter wywołuje invalidate() na main wątku
            }
        }
    }

    // ----- Overlay window -----

    private fun attachOverlay() {
        val px = resources.displayMetrics.density
        val params = WindowManager.LayoutParams(
            (OVERLAY_W_DP * px).toInt(),
            (OVERLAY_H_DP * px).toInt(),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 40
            y = 200
        }
        overlayView = OverlayView(this, windowManager, params)
        windowManager.addView(overlayView, params)
    }

    // ----- Notification -----

    private fun ensureNotificationChannel() {
        val ch = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notif_channel_desc)
            setShowBadge(false)
        }
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch)
    }

    private fun buildNotification(): Notification {
        val stopPi = PendingIntent.getService(
            this, 0,
            Intent(this, OverlayService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE
        )
        val stopAction = Notification.Action.Builder(
            Icon.createWithResource(this, R.drawable.ic_notification),
            getString(R.string.notif_action_stop),
            stopPi
        ).build()
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(getString(R.string.notif_text))
            .setSmallIcon(R.drawable.ic_notification)
            .addAction(stopAction)
            .setOngoing(true)
            .build()
    }

    // ----- Companion -----

    companion object {
        private const val CHANNEL_ID      = "tbh_overlay"
        private const val NOTIFICATION_ID = 1
        private const val OVERLAY_W_DP    = 240
        private const val OVERLAY_H_DP    = 135
        private const val TICK_MS         = 1500L
        private const val ACTION_STOP     = "com.tbh.mobile.STOP_OVERLAY"

        fun start(context: Context) =
            context.startForegroundService(Intent(context, OverlayService::class.java))

        fun stop(context: Context) =
            context.stopService(Intent(context, OverlayService::class.java))
    }
}
