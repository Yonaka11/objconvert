package com.mikazuki.pocketfamiliar.model

/**
 * Snapshot of all user-configurable settings for the pet.
 * Immutable data class; new instances are emitted when any field changes.
 */
data class PetSettings(
    /** Scale factor applied to the pet drawable. 1.0 = default size (64dp). */
    val petSize: Float = 1.0f,
    /** Multiplier applied to the base walking speed. 1.0 = normal speed. */
    val movementSpeed: Float = 1.0f,
    /** Whether the pet may enter the SLEEP state after idling. */
    val sleepEnabled: Boolean = true,
    /** ID of the selected pet profile. Reserved for future multi-pet support. */
    val selectedPetId: String = "default",
) {
    companion object {
        val DEFAULT = PetSettings()

        const val PET_SIZE_MIN = 0.5f
        const val PET_SIZE_MAX = 3.0f
        const val SPEED_MIN = 0.25f
        const val SPEED_MAX = 3.0f
    }
}
