package com.mikazuki.pocketfamiliar.pet.animation

import androidx.annotation.DrawableRes

/**
 * Describes a looping or one-shot sprite animation.
 *
 * [frames] is an ordered list of drawable resource IDs.
 * [frameDurationMs] is how long each frame is displayed.
 * [loop] controls whether the animation repeats.
 * [flipHorizontal] applies a mirror transform so a single walk sprite can serve
 * both left and right directions without duplicating art.
 */
data class PetAnimation(
    @DrawableRes val frames: List<Int>,
    val frameDurationMs: Long = 200L,
    val loop: Boolean = true,
    val flipHorizontal: Boolean = false
) {
    init {
        require(frames.isNotEmpty()) { "PetAnimation must have at least one frame." }
        require(frameDurationMs > 0) { "frameDurationMs must be positive." }
    }

    /** Total duration of one full cycle (non-looping: plays once; looping: repeats). */
    val cycleDurationMs: Long get() = frameDurationMs * frames.size
}

/**
 * Animator state used by PetView to step through animation frames.
 *
 * Call [currentFrame] with the current elapsed millis to get the drawable ID
 * that should be rendered this frame.
 */
class PetAnimator(private val animation: PetAnimation) {
    private var startTimeMs: Long = 0L
    private var started = false

    fun start(nowMs: Long = System.currentTimeMillis()) {
        startTimeMs = nowMs
        started = true
    }

    fun currentFrame(nowMs: Long = System.currentTimeMillis()): Int {
        if (!started) return animation.frames.first()
        val elapsed = nowMs - startTimeMs
        val frameIndex = if (animation.loop) {
            ((elapsed / animation.frameDurationMs) % animation.frames.size).toInt()
        } else {
            minOf((elapsed / animation.frameDurationMs).toInt(), animation.frames.size - 1)
        }
        return animation.frames[frameIndex]
    }

    val flipHorizontal: Boolean get() = animation.flipHorizontal
}
