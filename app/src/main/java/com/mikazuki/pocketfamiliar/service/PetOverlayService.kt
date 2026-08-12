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
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.getSystemService
import com.mikazuki.pocketfamiliar.MainActivity
import com.mikazuki.pocketfamiliar.R
import com.mikazuki.pocketfamiliar.overlay.PetOverlayManager
import com.mikazuki.pocketfamiliar.util.OverlayPermission
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PetOverlayService : Service() {
    private lateinit var overlayManager: PetOverlayManager

    override fun onCreate() {
        super.onCreate()
        overlayManager = PetOverlayManager(applicationContext)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopOverlay()
            return START_NOT_STICKY
        }

        if (!startForegroundSafely()) {
            return START_NOT_STICKY
        }

        if (!OverlayPermission.isGranted(this)) {
            Log.w(TAG, "Overlay permission is missing; stopping service.")
            stopOverlay()
            return START_NOT_STICKY
        }

        if (!overlayManager.isShowing && !overlayManager.show()) {
            Log.e(TAG, "Unable to show overlay; stopping service.")
            stopOverlay()
            return START_NOT_STICKY
        }

        _isRunning.value = true
        return START_STICKY
    }

    override fun onDestroy() {
        overlayManager.remove()
        _isRunning.value = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundSafely(): Boolean =
        try {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                buildNotification(),
                foregroundServiceType(),
            )
            true
        } catch (exception: RuntimeException) {
            Log.e(TAG, "Failed to promote pet overlay service to foreground.", exception)
            stopOverlay()
            false
        }

    private fun stopOverlay() {
        overlayManager.remove()
        _isRunning.value = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun foregroundServiceType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }

    private fun buildNotification(): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            REQUEST_OPEN_APP,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stopIntent = PendingIntent.getService(
            this,
            REQUEST_STOP_SERVICE,
            stopIntent(this),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_familiar)
            .setContentTitle(getString(R.string.foreground_service_notification_title))
            .setContentText(getString(R.string.foreground_service_notification_text))
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                R.drawable.ic_stat_familiar,
                getString(R.string.foreground_service_stop_action),
                stopIntent,
            )
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            getString(R.string.foreground_service_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.foreground_service_channel_description)
        }

        getSystemService<NotificationManager>()?.createNotificationChannel(channel)
    }

    companion object {
        private const val TAG = "PetOverlayService"
        private const val ACTION_START = "com.mikazuki.pocketfamiliar.action.START_PET"
        private const val ACTION_STOP = "com.mikazuki.pocketfamiliar.action.STOP_PET"
        private const val NOTIFICATION_CHANNEL_ID = "pocket_familiar_overlay"
        private const val NOTIFICATION_ID = 1001
        private const val REQUEST_OPEN_APP = 2001
        private const val REQUEST_STOP_SERVICE = 2002

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        fun startIntent(context: Context): Intent =
            Intent(context, PetOverlayService::class.java).setAction(ACTION_START)

        fun stopIntent(context: Context): Intent =
            Intent(context, PetOverlayService::class.java).setAction(ACTION_STOP)
    }
}
