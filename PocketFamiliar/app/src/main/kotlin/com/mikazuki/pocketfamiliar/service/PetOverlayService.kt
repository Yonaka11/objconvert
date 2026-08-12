package com.mikazuki.pocketfamiliar.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import com.mikazuki.pocketfamiliar.MainActivity
import com.mikazuki.pocketfamiliar.R
import com.mikazuki.pocketfamiliar.data.PetSettingsRepository
import com.mikazuki.pocketfamiliar.model.PetSettings
import com.mikazuki.pocketfamiliar.overlay.PetOverlayManager
import com.mikazuki.pocketfamiliar.pet.behavior.PetState
import com.mikazuki.pocketfamiliar.pet.behavior.PetStateMachine
import com.mikazuki.pocketfamiliar.pet.physics.ForcedTransition
import com.mikazuki.pocketfamiliar.pet.physics.PetPhysicsEngine
import com.mikazuki.pocketfamiliar.util.BatteryMonitor
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
private const val CHANNEL_ID = "pocket_familiar_overlay"
private const val TICK_INTERVAL_MS = 16L   // ~60 fps

const val ACTION_STOP_SERVICE = "com.mikazuki.pocketfamiliar.STOP"

class PetOverlayService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private lateinit var overlayManager: PetOverlayManager
    private lateinit var physics: PetPhysicsEngine
    private lateinit var stateMachine: PetStateMachine
    private lateinit var batteryMonitor: BatteryMonitor
    private lateinit var settingsRepository: PetSettingsRepository

    private var settings: PetSettings = PetSettings()
    private var tickJob: Job? = null

    // -------------------------------------------------------------------------
    // Service lifecycle
    // -------------------------------------------------------------------------

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")

        settingsRepository = PetSettingsRepository(applicationContext)
        batteryMonitor = BatteryMonitor(applicationContext)

        physics = PetPhysicsEngine()

        stateMachine = PetStateMachine(
            scope = serviceScope,
            onStateChanged = ::onPetStateChanged,
            isSleepEnabled = { settings.sleepEnabled },
        )

        overlayManager = PetOverlayManager(
            context = applicationContext,
            onDragStarted = ::onDragStarted,
            onDragReleased = ::onDragReleased,
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SERVICE) {
            stopSelf()
            return START_NOT_STICKY
        }

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        // Guard: overlay permission might have been revoked
        if (!Settings.canDrawOverlays(this)) {
            Log.e(TAG, "Overlay permission not granted — stopping service")
            stopSelf()
            return START_NOT_STICKY
        }

        batteryMonitor.register()

        // Load persisted settings before creating the overlay
        serviceScope.launch {
            settings = settingsRepository.settingsFlow.first()
            startOverlay()

            // React to settings changes while running
            settingsRepository.settingsFlow.collect { newSettings ->
                val sizeChanged = newSettings.petSize != settings.petSize
                settings = newSettings
                if (sizeChanged) overlayManager.updatePetSize(newSettings.petSize)
                physics.velocityX = 0f  // speed changes take effect on next tick
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
        tickJob?.cancel()
        stateMachine.stop()
        batteryMonitor.unregister()
        overlayManager.remove()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // -------------------------------------------------------------------------
    // Overlay initialisation
    // -------------------------------------------------------------------------

    private fun startOverlay() {
        overlayManager.create(settings.petSize)

        // Sync physics with actual screen dimensions
        physics.screenWidth = overlayManager.getScreenWidth()
        physics.screenHeight = overlayManager.getScreenHeight()
        physics.petWidth = overlayManager.petSizePx
        physics.petHeight = overlayManager.petSizePx

        // Start at horizontal centre, one quarter down
        physics.x = (physics.screenWidth / 2 - physics.petWidth / 2).toFloat()
        physics.y = (physics.screenHeight / 4).toFloat()

        stateMachine.start()
        startTickLoop()
    }

    // -------------------------------------------------------------------------
    // Tick loop (~60 fps)
    // -------------------------------------------------------------------------

    private fun startTickLoop() {
        tickJob?.cancel()
        tickJob = serviceScope.launch {
            var lastMs = System.currentTimeMillis()
            while (true) {
                delay(TICK_INTERVAL_MS)
                val now = System.currentTimeMillis()
                val delta = (now - lastMs) / 1000f
                lastMs = now
                onTick(delta)
            }
        }
    }

    private fun onTick(deltaSeconds: Float) {
        // Advance physics and check for boundary events
        val forcedTransition = physics.update(
            currentState = stateMachine.currentState,
            deltaSeconds = deltaSeconds,
            movementSpeed = settings.movementSpeed,
        )
        forcedTransition?.let { onForcedTransition(it) }

        // Push updated position to window manager
        overlayManager.updatePosition(physics.x, physics.y)

        // Advance the sprite animation
        overlayManager.tick()
    }

    // -------------------------------------------------------------------------
    // State callbacks
    // -------------------------------------------------------------------------

    private fun onPetStateChanged(state: PetState) {
        overlayManager.applyState(state)
    }

    private fun onDragStarted() {
        stateMachine.forceState(PetState.Dragged)
        // Sync physics position from the current window params before drag begins
        // (PetOverlayManager updates params directly during drag, so physics lags;
        //  we reconcile on drag release instead)
    }

    private fun onDragReleased(releaseVelocityY: Float) {
        // Reconcile physics.x/y with where the view ended up after the drag.
        // PetOverlayManager updates LayoutParams.x/y in place during touch events;
        // we read those back via a dedicated accessor.
        val pos = overlayManager.getDragPosition()
        physics.x = pos.first
        physics.y = pos.second
        physics.onDragReleased(releaseVelocityY)
        stateMachine.forceState(PetState.Falling)
    }

    private fun onForcedTransition(transition: ForcedTransition) {
        when (transition) {
            ForcedTransition.TurnLeft -> stateMachine.forceState(PetState.WalkLeft)
            ForcedTransition.TurnRight -> stateMachine.forceState(PetState.WalkRight)
            ForcedTransition.Land -> stateMachine.forceState(PetState.Idle)
        }
    }


    // -------------------------------------------------------------------------
    // Notification
    // -------------------------------------------------------------------------

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.notification_channel_description)
            setShowBadge(false)
        }
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )

        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, PetOverlayService::class.java).apply {
                action = ACTION_STOP_SERVICE
            },
            PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_pet_idle)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setContentIntent(openAppIntent)
            .addAction(
                R.drawable.ic_pet_idle,
                getString(R.string.notification_action_stop),
                stopIntent,
            )
            .setOngoing(true)
            .setSilent(true)
            .build()
    }
}
