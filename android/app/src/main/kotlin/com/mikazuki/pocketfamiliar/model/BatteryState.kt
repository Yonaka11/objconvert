package com.mikazuki.pocketfamiliar.model

/**
 * Snapshot of device battery state.
 * Used for future battery-reactive pet behavior.
 */
data class BatteryState(
    val levelPercent: Int = 100,
    val isCharging: Boolean = false,
    val isLow: Boolean = false,
) {
    /** Semantic mood bucket based on battery level and charging state. */
    val mood: BatteryMood get() = when {
        isCharging -> BatteryMood.EXCITED
        levelPercent >= 80 -> BatteryMood.HAPPY
        levelPercent >= 40 -> BatteryMood.NORMAL
        levelPercent >= 15 -> BatteryMood.TIRED
        else -> BatteryMood.WORRIED
    }

    companion object {
        val UNKNOWN = BatteryState()
    }
}

enum class BatteryMood {
    EXCITED,  // Charging
    HAPPY,    // 80–100%
    NORMAL,   // 40–79%
    TIRED,    // 15–39%
    WORRIED,  // 0–14%
}
