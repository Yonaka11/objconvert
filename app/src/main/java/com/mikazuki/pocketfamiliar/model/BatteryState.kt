package com.mikazuki.pocketfamiliar.model

enum class BatteryMood {
    HAPPY,    // 80–100%, not charging
    NORMAL,   // 40–79%
    TIRED,    // 15–39%
    SLEEPY,   // 0–14%
    EXCITED   // charging
}

data class BatteryState(
    val levelPercent: Int = 100,
    val isCharging: Boolean = false,
    val isLow: Boolean = false
) {
    val mood: BatteryMood
        get() = when {
            isCharging -> BatteryMood.EXCITED
            levelPercent >= 80 -> BatteryMood.HAPPY
            levelPercent >= 40 -> BatteryMood.NORMAL
            levelPercent >= 15 -> BatteryMood.TIRED
            else -> BatteryMood.SLEEPY
        }
}
