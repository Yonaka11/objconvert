package com.mikazuki.pocketfamiliar.pet.animation

import com.mikazuki.pocketfamiliar.R
import com.mikazuki.pocketfamiliar.pet.PetState

/**
 * Registry of all animation clips for the default placeholder pet.
 *
 * When real sprite sheets are available, replace the [R.drawable.ic_pet_placeholder]
 * references with the actual frame drawables and adjust [PetAnimation.frameDurationMs].
 *
 * The walkLeft clip re-uses the walkRight art with [PetAnimation.flipHorizontal] = true.
 */
object PetAnimations {

    val idle = PetAnimation(
        frames = listOf(R.drawable.ic_pet_placeholder),
        frameDurationMs = 500L,
        loop = true,
    )

    val walkRight = PetAnimation(
        frames = listOf(R.drawable.ic_pet_placeholder),
        frameDurationMs = 150L,
        loop = true,
        flipHorizontal = false,
    )

    val walkLeft = PetAnimation(
        frames = listOf(R.drawable.ic_pet_placeholder),
        frameDurationMs = 150L,
        loop = true,
        flipHorizontal = true,
    )

    val sleep = PetAnimation(
        frames = listOf(R.drawable.ic_pet_placeholder),
        frameDurationMs = 1000L,
        loop = true,
    )

    val falling = PetAnimation(
        frames = listOf(R.drawable.ic_pet_placeholder),
        frameDurationMs = 100L,
        loop = true,
    )

    /** Returns the appropriate animation for a given [PetState]. */
    fun forState(state: PetState): PetAnimation = when (state) {
        PetState.IDLE -> idle
        PetState.WALK_LEFT -> walkLeft
        PetState.WALK_RIGHT -> walkRight
        PetState.SLEEP -> sleep
        PetState.FALLING, PetState.DRAGGED -> falling
        else -> idle
    }
}
