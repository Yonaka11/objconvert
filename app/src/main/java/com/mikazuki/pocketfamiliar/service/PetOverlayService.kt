package com.mikazuki.pocketfamiliar.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.mikazuki.pocketfamiliar.MainActivity
import com.mikazuki.pocketfamiliar.R
import com.mikazuki.pocketfamiliar.data.PetSettingsRepository
import com.mikazuki.pocketfamiliar.model.PetSettings
import com.mikazuki.pocketfamiliar.overlay.PetOverlayManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

private const val TAG = "PetOverlayService"
private const val NOTIFICATION_ID = 1001
private const val CHANNEL_ID = "pocket_familiar_overlay"
const val ACTION_STOP_PET = "com.mikazuki.pocketfamiliar.STOP_PET"

/**
 * Foreground service that owns the pet overlay.
 *
 * Lifecycle:
 *  onCreate  → create notification channel
 *  onStartCommand(null/default) → start foreground, load settings, show overlay
 *  onStartCommand(ACTION_STOP_PET) → stopSelf()
 *  onDestroy → remove overlay, cancel coroutines
 *
 * Screen-rotation handling: a BroadcastReceiver listens for
 * ACTION_CONFIGURATION_CHANGED and calls [PetOverlayManager.onScreenBoundsChanged].
 */
class PetOverlayService : Service() {

    private val serviceJob = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + serviceJob)

    private var overlayManager: PetOverlayManager? = null
    private lateinit var settingsRepository: PetSettingsRepository

    private val configChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_CONFIGURATION_CHANGED) {
                overlayManager?.onScreenBoundsChanged()
            }
        }
    }

    companion object {
        /** Polled by the UI to show pet-active state without requiring service binding. */
        @Volatile
        var isRunning: Boolean = false
            private set
    }

    override fun onCreate() {
        super.onCreate()
        settingsRepository = PetSettingsRepository(this)
        createNotificationChannel()
        registerReceiver(configChangeReceiver, IntentFilter(Intent.ACTION_CONFIGURATION_CHANGED))
        Log.d(TAG, "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_PET) {
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = buildNotification()
        startForegroundCompat(notification)

        if (overlayManager == null) {
            scope.launch {
                val settings = runCatching { settingsRepository.settingsFlow.first() }
                    .getOrDefault(PetSettings())
                val manager = PetOverlayManager(this@PetOverlayService, scope)
                overlayManager = manager
                manager.show(settings)
            }
        }

        isRunning = true
        return START_STICKY
    }

    override fun onDestroy() {
        Log.d(TAG, "Service destroying")
        overlayManager?.hide()
        overlayManager = null
        scope.cancel()
        isRunning = false
        try {
            unregisterReceiver(configChangeReceiver)
        } catch (e: IllegalArgumentException) {
            // Receiver was never registered or already unregistered
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_channel_description)
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val openPendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, PetOverlayService::class.java).apply {
                action = ACTION_STOP_PET
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.notification_action_stop),
                stopPendingIntent
            )
            .build()
    }
}
