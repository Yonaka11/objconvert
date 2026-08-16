package com.mikazuki.pocketfamiliar.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.mikazuki.pocketfamiliar.model.BatteryState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Monitors device battery state and exposes it as a [StateFlow].
 *
 * Call [start] to begin receiving updates and [stop] to clean up.
 * The initial value is read immediately via a sticky broadcast — no delay.
 *
 * The pet can later react to [BatteryState.mood] for expressive behavior
 * (e.g. excited while charging, sleepy when critically low).
 */
class BatteryMonitor(private val context: Context) {

    private val _batteryState = MutableStateFlow(readCurrentState())
    val batteryState: StateFlow<BatteryState> = _batteryState

    private var isStarted = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            _batteryState.value = parseIntent(intent)
        }
    }

    fun start() {
        if (isStarted) return
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        isStarted = true
    }

    fun stop() {
        if (!isStarted) return
        try {
            context.unregisterReceiver(receiver)
        } catch (_: IllegalArgumentException) { /* already unregistered */ }
        isStarted = false
    }

    private fun readCurrentState(): BatteryState {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?: return BatteryState()
        return parseIntent(intent)
    }

    private fun parseIntent(intent: Intent): BatteryState {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        val levelPercent = if (level >= 0 && scale > 0) (level * 100 / scale) else 0

        val status = intent.getIntExtra(
            BatteryManager.EXTRA_STATUS,
            BatteryManager.BATTERY_STATUS_UNKNOWN
        )
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        return BatteryState(
            levelPercent = levelPercent,
            isCharging = isCharging,
            isLow = levelPercent < 15
        )
    }
}
