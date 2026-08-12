package com.mikazuki.pocketfamiliar.util

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.net.toUri

object OverlayPermission {
    fun isGranted(context: Context): Boolean = Settings.canDrawOverlays(context)

    fun settingsIntent(context: Context): Intent =
        Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            "package:${context.packageName}".toUri(),
        )
}
