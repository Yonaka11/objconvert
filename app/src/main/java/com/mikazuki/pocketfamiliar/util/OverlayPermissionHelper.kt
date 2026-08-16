package com.mikazuki.pocketfamiliar.util

import android.content.Context
import android.provider.Settings

object OverlayPermissionHelper {
    fun hasPermission(context: Context): Boolean = Settings.canDrawOverlays(context)
}
