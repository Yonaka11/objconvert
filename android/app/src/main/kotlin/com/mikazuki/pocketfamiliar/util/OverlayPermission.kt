package com.mikazuki.pocketfamiliar.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

object OverlayPermission {

    /** Returns true if the app currently has the SYSTEM_ALERT_WINDOW permission. */
    fun isGranted(context: Context): Boolean = Settings.canDrawOverlays(context)

    /**
     * Returns an [Intent] that opens the system screen where the user can
     * toggle "Display over other apps" for this specific package.
     */
    fun buildSettingsIntent(context: Context): Intent = Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.fromParts("package", context.packageName, null)
    )
}
