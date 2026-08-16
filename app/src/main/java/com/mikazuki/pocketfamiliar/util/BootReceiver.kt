package com.mikazuki.pocketfamiliar.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Placeholder boot receiver.
 *
 * The receiver is declared in the manifest but disabled by default
 * (android:enabled="false"). It will be enabled programmatically when the
 * user toggles "Start automatically on reboot" in settings.
 *
 * Full implementation: read DataStore startOnBoot flag and start
 * PetOverlayService if true.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("BootReceiver", "Boot received — auto-start not yet implemented")
        // TODO (next iteration): read PetSettingsRepository.startOnBoot and
        //  call context.startForegroundService(Intent(context, PetOverlayService::class.java))
    }
}
