package com.mikazuki.pocketfamiliar.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.mikazuki.pocketfamiliar.model.BatteryState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Observes device battery state using sticky broadcasts.
 * No special permissions are required; ACTION_BATTERY_CHANGED is a sticky broadcast
 * available to all apps without declaring any permission.
 *
 * Call [start] once when the host service starts and [stop] when it stops.
 */
class BatteryMonitor(private val context: Context) {

    private val _state = MutableStateFlow(BatteryState.UNKNOWN)
    val state: StateFlow<BatteryState> = _state.asStateFlow()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            _state.value = intent.toBatteryState()
        }
    }

    fun start() {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        // ACTION_BATTERY_CHANGED is sticky; registerReceiver returns the last sticky intent immediately.
        val sticky = context.registerReceiver(receiver, filter)
        if (sticky != null) {
            _state.value = sticky.toBatteryState()
        }
    }

    fun stop() {
        try {
            context.unregisterReceiver(receiver)
        } catch (_: IllegalArgumentException) {
            // Receiver was not registered; safe to ignore.
        }
    }

    private fun Intent.toBatteryState(): BatteryState {
        val level = getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        val percent = if (level >= 0 && scale > 0) (level * 100 / scale) else 100

        val status = getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val isLow = getBooleanExtra(BatteryManager.EXTRA_BATTERY_LOW, false)

        return BatteryState(
            levelPercent = percent,
            isCharging = isCharging,
            isLow = isLow,
        )
    }
}
