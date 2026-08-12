package com.mikazuki.pocketfamiliar.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.mikazuki.pocketfamiliar.MainActivity
import com.mikazuki.pocketfamiliar.R
import com.mikazuki.pocketfamiliar.data.PetSettingsRepository
import com.mikazuki.pocketfamiliar.model.PetSettings
import com.mikazuki.pocketfamiliar.overlay.PetOverlayManager
import com.mikazuki.pocketfamiliar.pet.behavior.PetController
import com.mikazuki.pocketfamiliar.pet.physics.PetPhysicsEngine
import com.mikazuki.pocketfamiliar.util.BatteryMonitor
import com.mikazuki.pocketfamiliar.util.OverlayPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val TAG = "PetOverlayService"
private const val NOTIFICATION_ID = 1001
private const val FRAME_RATE_MS = 33L // ~30 fps

/**
 * Foreground service that owns the pet overlay for its entire lifetime.
 *
 * Lifecycle:
 *  1. [onStartCommand] with [ACTION_START]: creates overlay, starts physics/behavior loop
 *  2. [onStartCommand] with [ACTION_STOP] or notification "Stop" tap: tears everything down
 *  3. [onDestroy]: final cleanup safety net
 *
 * The coroutine scope is [SupervisorJob] so one failing child doesn't cancel others.
 */
class PetOverlayService : Service() {

    companion object {
        const val ACTION_START = "com.mikazuki.pocketfamiliar.START_PET"
        const val ACTION_STOP = "com.mikazuki.pocketfamiliar.STOP_PET"

        private const val BASE_WALK_SPEED = 120f

        /** Returns an Intent to start the pet. */
        fun startIntent(context: Context) =
            Intent(context, PetOverlayService::class.java).apply { action = ACTION_START }

        /** Returns an Intent to stop the pet. */
        fun stopIntent(context: Context) =
            Intent(context, PetOverlayService::class.java).apply { action = ACTION_STOP }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private lateinit var windowManager: WindowManager
    private lateinit var physics: PetPhysicsEngine
    private lateinit var controller: PetController
    private lateinit var overlayManager: PetOverlayManager
    private lateinit var batteryMonitor: BatteryMonitor
    private lateinit var settingsRepo: PetSettingsRepository

    private var frameLoopJob: Job? = null
    private var settingsJob: Job? = null
    private var serviceStartMs = System.currentTimeMillis()

    // ── Service lifecycle ─────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        settingsRepo = PetSettingsRepository(applicationContext)
        batteryMonitor = BatteryMonitor(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return when (intent?.action) {
            ACTION_START -> {
                handleStart()
                START_STICKY
            }
            ACTION_STOP -> {
                handleStop()
                START_NOT_STICKY
            }
            else -> {
                Log.w(TAG, "Unknown action: ${intent?.action}")
                START_NOT_STICKY
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        tearDown()
    }

    // ── Start / stop logic ────────────────────────────────────────────────

    private fun handleStart() {
        if (!OverlayPermission.isGranted(this)) {
            Log.e(TAG, "Overlay permission not granted; stopping service")
            stopSelf()
            return
        }

        // Already running — ignore duplicate start
        if (frameLoopJob?.isActive == true) {
            Log.d(TAG, "Service already running; ignoring duplicate start")
            return
        }

        serviceStartMs = System.currentTimeMillis()
        createNotificationChannel()
        startForegroundWithNotification()

        physics = PetPhysicsEngine()
        controller = PetController(serviceScope, physics)
        overlayManager = PetOverlayManager(this, windowManager, physics, controller)

        batteryMonitor.start()
        overlayManager.attach()
        controller.start()

        // Apply persisted settings once before starting the loop
        serviceScope.launch {
            val settings = settingsRepo.settingsFlow.first()
            applySettings(settings)
        }

        // Watch for settings changes while running
        settingsJob = serviceScope.launch {
            settingsRepo.settingsFlow.collect { settings ->
                applySettings(settings)
            }
        }

        // Main animation + physics loop
        frameLoopJob = serviceScope.launch {
            var lastFrameMs = System.currentTimeMillis()
            while (true) {
                val now = System.currentTimeMillis()
                val deltaMs = now - lastFrameMs
                lastFrameMs = now
                val deltaSeconds = deltaMs / 1000f

                val state = controller.state.value

                // Advance physics
                physics.update(state, deltaSeconds)

                // Let the controller react to physics results
                controller.onPhysicsUpdate()

                // Render
                val elapsed = now - serviceStartMs
                overlayManager.onFrame(controller.state.value, elapsed)

                delay(FRAME_RATE_MS)
            }
        }

        Log.i(TAG, "Pet overlay service started")
    }

    private fun handleStop() {
        Log.i(TAG, "Stop requested")
        tearDown()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun tearDown() {
        frameLoopJob?.cancel()
        frameLoopJob = null

        settingsJob?.cancel()
        settingsJob = null

        if (::controller.isInitialized) controller.stop()
        if (::overlayManager.isInitialized) overlayManager.detach()
        if (::batteryMonitor.isInitialized) batteryMonitor.stop()

        serviceScope.cancel()
    }

    private fun applySettings(settings: PetSettings) {
        if (!::overlayManager.isInitialized) return
        overlayManager.updateSize(settings.petSize)
        if (::controller.isInitialized) {
            controller.setSleepEnabled(settings.sleepEnabled)
        }
        if (::physics.isInitialized) {
            physics.walkSpeed = BASE_WALK_SPEED * settings.movementSpeed
        }
    }

    // ── Notification ──────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            getString(R.string.notification_channel_id),
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_channel_description)
            setShowBadge(false)
            enableVibration(false)
            enableLights(false)
        }
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val stopPendingIntent = PendingIntent.getService(
            this, 1,
            stopIntent(this),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        return NotificationCompat.Builder(this, getString(R.string.notification_channel_id))
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.drawable.ic_notification_pet)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setSilent(true)
            .addAction(
                NotificationCompat.Action.Builder(
                    0,
                    getString(R.string.notification_action_stop),
                    stopPendingIntent,
                ).build()
            )
            .build()
    }

    private fun startForegroundWithNotification() {
        val notification = buildNotification()
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            else
                0,
        )
    }

}
