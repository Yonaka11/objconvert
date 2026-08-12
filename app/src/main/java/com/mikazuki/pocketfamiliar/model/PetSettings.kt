package com.mikazuki.pocketfamiliar.model

data class PetSettings(
    val petSizeDp: Float = 80f,
    val speedMultiplier: Float = 1.0f,
    val sleepEnabled: Boolean = true,
    val startOnBoot: Boolean = false,
    val selectedPetId: String = "default"
)
