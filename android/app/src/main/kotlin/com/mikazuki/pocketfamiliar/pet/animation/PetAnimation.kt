package com.mikazuki.pocketfamiliar.pet.animation

/**
 * Describes one animation clip as a sequence of drawable resource IDs.
 *
 * The animation system is intentionally simple for the MVP:
 * each clip cycles through [frames] with a fixed [frameDurationMs] per frame.
 * Sprite sheets are not required; individual drawables are fine.
 *
 * @param frames         List of drawable resource IDs that make up the clip.
 * @param frameDurationMs Milliseconds to hold each frame before advancing.
 * @param loop           Whether the animation restarts after the last frame.
 * @param flipHorizontal When true the renderer mirrors the drawable on the X axis,
 *                       allowing left/right walk animations to share the same art.
 */
data class PetAnimation(
    val frames: List<Int>,
    val frameDurationMs: Long = 150L,
    val loop: Boolean = true,
    val flipHorizontal: Boolean = false,
) {
    init {
        require(frames.isNotEmpty()) { "PetAnimation must have at least one frame." }
    }

    /** Returns the drawable resource ID for a given elapsed time. */
    fun frameAt(elapsedMs: Long): Int {
        if (frames.size == 1) return frames[0]
        val totalDuration = frameDurationMs * frames.size
        val time = if (loop) elapsedMs % totalDuration else elapsedMs.coerceAtMost(totalDuration - 1)
        val index = (time / frameDurationMs).toInt().coerceIn(0, frames.lastIndex)
        return frames[index]
    }
}
