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
import android.view.View
import android.view.WindowManager
import com.tbh.core.GameState
import com.tbh.mobile.R
import com.tbh.mobile.menu.MenuActivity
import com.tbh.mobile.overlay.OverlayView
import com.tbh.mobile.overlay.displayName
import com.tbh.mobile.state.GameRepository
import com.tbh.mobile.state.MenuVisibility
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: OverlayView? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        ensureNotificationChannel()
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        attachOverlay()
        loadAndApplyOffline()
        observeState()
        observeMenuVisibility()
        startGameLoop()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) stopSelf()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        GameRepository.saveBlocking()   // best-effort zapis przed zamknięciem
        scope.cancel()
        overlayView?.let { windowManager.removeView(it) }
        overlayView = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ----- Stan: jedno źródło prawdy w GameRepository -----

    /** Wczytanie zapisu + naliczenie postępu offline (toast powitalny). */
    private fun loadAndApplyOffline() {
        val result = GameRepository.init(applicationContext, TICK_MS) ?: return
        if (result.ticksSimulated > 0 && (result.goldGained > 0 || result.monstersDefeated > 0)) {
            val defeated = result.monstersDefeated.coerceAtLeast(0)
            overlayView?.showToast(
                "Podczas nieobecności: +${result.goldGained} złota, pokonano $defeated",
                WELCOME_TOAST_MS
            )
        }
    }

    /** Renderowanie nakładki z każdego nowego stanu — także po equip z menu (realtime). */
    private fun observeState() {
        scope.launch {
            var previous = GameRepository.state.value
            overlayView?.state = previous
            GameRepository.state.collect { new ->
                overlayView?.state = new
                detectAndShowEvents(previous, new)
                previous = new
            }
        }
    }

    /** Chowa nakładkę, gdy pełnoekranowe menu jest na wierzchu, i pokazuje po jego zamknięciu. */
    private fun observeMenuVisibility() {
        scope.launch {
            MenuVisibility.isOpen.collect { open ->
                overlayView?.visibility = if (open) View.GONE else View.VISIBLE
            }
        }
    }

    /** Pętla gry — wyłącznie wołanie tick() + okresowy zapis. Logika walki w :core. */
    private fun startGameLoop() {
        scope.launch {
            var ticksSinceSave = 0
            while (isActive) {
                delay(TICK_MS)
                GameRepository.tick()
                if (++ticksSinceSave >= SAVE_EVERY_TICKS) {
                    ticksSinceSave = 0
                    GameRepository.saveAsync()
                }
            }
        }
    }

    private fun detectAndShowEvents(old: GameState, new: GameState) {
        val view = overlayView ?: return

        // Złoto
        val goldDiff = new.gold - old.gold
        if (goldDiff > 0) view.showToast("+$goldDiff złota")

        // Level up dowolnego bohatera
        new.heroes.forEachIndexed { i, newHero ->
            val oldHero = old.heroes.getOrNull(i) ?: return@forEachIndexed
            if (newHero.level > oldHero.level) {
                view.showToast("${newHero.heroClass.displayName()} → poziom ${newHero.level}!")
            }
        }

        // Nowy przedmiot w ekwipunku
        if (new.inventory.size > old.inventory.size) {
            val item = new.inventory.last()
            view.showToast("Zdobyto: ${item.rarity.displayName()} ${item.name}")
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
        overlayView = OverlayView(this, windowManager, params).apply {
            onTap = { launchMenu() }
        }
        windowManager.addView(overlayView, params)
    }

    /** Tap nakładki (nie drag) → pełnoekranowe menu. BAL dozwolony dzięki SYSTEM_ALERT_WINDOW. */
    private fun launchMenu() {
        val intent = Intent(this, MenuActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
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

    companion object {
        private const val CHANNEL_ID      = "tbh_overlay"
        private const val NOTIFICATION_ID = 1
        private const val OVERLAY_W_DP    = 240
        private const val OVERLAY_H_DP    = 135
        private const val TICK_MS         = 1500L
        private const val SAVE_EVERY_TICKS = 10
        private const val WELCOME_TOAST_MS = 5000L
        private const val ACTION_STOP     = "com.tbh.mobile.STOP_OVERLAY"

        fun start(context: Context) =
            context.startForegroundService(Intent(context, OverlayService::class.java))

        fun stop(context: Context) =
            context.stopService(Intent(context, OverlayService::class.java))
    }
}
