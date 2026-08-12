package com.mikazuki.pocketfamiliar.pet

/**
 * All possible behavioral states for a pet.
 *
 * The state machine lives in [behavior.PetController].
 * Physics runs independently in [physics.PetPhysicsEngine] and reacts to state changes.
 *
 * States marked FUTURE are not implemented in 0.1 but are reserved so animation/behavior
 * code can be added later without renaming anything.
 */
enum class PetState {
    // ── Active MVP states ─────────────────────────────────────────────────
    IDLE,
    WALK_LEFT,
    WALK_RIGHT,
    SLEEP,
    DRAGGED,
    FALLING,

    // ── Reserved for future releases ──────────────────────────────────────
    CLIMB_LEFT,
    CLIMB_RIGHT,
    HANGING,
    JUMPING,
    SITTING,
    EATING,
    PLAYING,
    CUSTOM;

    /** Returns true for any movement state where the pet moves horizontally. */
    val isWalking: Boolean get() = this == WALK_LEFT || this == WALK_RIGHT

    /** Returns true for states where physics/AI should not take control. */
    val isPlayerControlled: Boolean get() = this == DRAGGED
}
