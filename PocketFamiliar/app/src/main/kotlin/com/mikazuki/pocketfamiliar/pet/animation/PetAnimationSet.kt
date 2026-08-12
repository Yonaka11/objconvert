package com.mikazuki.pocketfamiliar.pet.animation

import com.mikazuki.pocketfamiliar.R
import com.mikazuki.pocketfamiliar.pet.behavior.PetState

/**
 * Maps each [PetState] to its [PetAnimation].
 *
 * Walk directions reuse the same frames; horizontal flipping is handled by
 * [PetView] via [android.view.View.setScaleX] so we don't need duplicate art.
 */
object PetAnimationSet {

    private val idle = PetAnimation(
        frames = listOf(R.drawable.ic_pet_idle),
        frameDurationMs = 500L,
    )

    private val walk = PetAnimation(
        frames = listOf(R.drawable.ic_pet_walk1, R.drawable.ic_pet_walk2),
        frameDurationMs = 180L,
    )

    private val sleep = PetAnimation(
        frames = listOf(R.drawable.ic_pet_sleep),
        frameDurationMs = 1000L,
    )

    private val fall = PetAnimation(
        frames = listOf(R.drawable.ic_pet_fall),
        frameDurationMs = 100L,
    )

    fun forState(state: PetState): PetAnimation = when (state) {
        is PetState.Idle -> idle
        is PetState.WalkLeft -> walk
        is PetState.WalkRight -> walk
        is PetState.Sleep -> sleep
        is PetState.Falling -> fall
        is PetState.Dragged -> fall  // Use fall sprite while being held
    }

    /** Whether the sprite should be horizontally flipped for this state. */
    fun isFlipped(state: PetState): Boolean = state is PetState.WalkLeft
}
