package com.mikazuki.pocketfamiliar.pet

/**
 * All possible states for a pet.
 *
 * The core MVP loop uses IDLE, WALK_LEFT, WALK_RIGHT, SLEEP, DRAGGED, and FALLING.
 * The remaining states scaffold future feature iterations without requiring
 * architectural changes.
 */
enum class PetState {
    IDLE,
    WALK_LEFT,
    WALK_RIGHT,
    SLEEP,
    DRAGGED,
    FALLING,

    // Future states — not yet implemented
    CLIMB_LEFT,
    CLIMB_RIGHT,
    HANGING,
    JUMPING,
    SITTING,
    EATING,
    PLAYING;

    /** True when the pet is moving autonomously (not user-controlled or physics-driven). */
    val isAutonomous: Boolean
        get() = this !in setOf(DRAGGED, FALLING)
}
