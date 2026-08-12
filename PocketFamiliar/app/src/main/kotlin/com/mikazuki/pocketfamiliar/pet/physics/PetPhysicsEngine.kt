package com.mikazuki.pocketfamiliar.pet.physics

import com.mikazuki.pocketfamiliar.pet.behavior.PetState

private const val GRAVITY = 1200f   // px/s² — feels snappy without being instant
private const val MAX_FALL_SPEED = 1800f

/**
 * Lightweight physics for the pet overlay.
 *
 * Operates in raw pixel space so it can be applied directly to
 * [WindowManager.LayoutParams] x/y coordinates.
 */
class PetPhysicsEngine {

    var x: Float = 0f
    var y: Float = 0f
    var velocityX: Float = 0f
    var velocityY: Float = 0f

    var screenWidth: Int = 1080
    var screenHeight: Int = 1920
    var petWidth: Int = 128
    var petHeight: Int = 128

    private val maxX get() = (screenWidth - petWidth).toFloat()
    private val maxY get() = (screenHeight - petHeight).toFloat()

    /**
     * Advance the physics simulation by [deltaSeconds].
     * Returns the state that should be forced (null = no forced transition).
     */
    fun update(currentState: PetState, deltaSeconds: Float, movementSpeed: Float): ForcedTransition? {
        return when (currentState) {
            is PetState.WalkLeft -> updateWalk(-movementSpeed, deltaSeconds)
            is PetState.WalkRight -> updateWalk(movementSpeed, deltaSeconds)
            is PetState.Falling -> updateFalling(deltaSeconds)
            else -> null
        }
    }

    private fun updateWalk(speed: Float, delta: Float): ForcedTransition? {
        x += speed * delta
        return when {
            x < 0f -> {
                x = 0f
                ForcedTransition.TurnRight
            }
            x > maxX -> {
                x = maxX
                ForcedTransition.TurnLeft
            }
            else -> null
        }
    }

    private fun updateFalling(delta: Float): ForcedTransition? {
        velocityY = (velocityY + GRAVITY * delta).coerceAtMost(MAX_FALL_SPEED)
        y += velocityY * delta

        x = x.coerceIn(0f, maxX)

        return if (y >= maxY) {
            y = maxY
            velocityY = 0f
            ForcedTransition.Land
        } else null
    }

    /** Apply a drag position, clamping to screen edges. */
    fun applyDragPosition(rawX: Float, rawY: Float) {
        x = rawX.coerceIn(0f, maxX)
        y = rawY.coerceIn(0f, maxY)
    }

    /** Called when drag is released; sets the initial fall velocity. */
    fun onDragReleased(releaseVelocityY: Float = 0f) {
        velocityX = 0f
        velocityY = releaseVelocityY.coerceIn(0f, MAX_FALL_SPEED)
    }

    /** Update screen dimensions and re-clamp the pet position. */
    fun onScreenSizeChanged(newWidth: Int, newHeight: Int) {
        screenWidth = newWidth
        screenHeight = newHeight
        x = x.coerceIn(0f, maxX)
        y = y.coerceIn(0f, maxY)
    }
}

enum class ForcedTransition {
    TurnLeft, TurnRight, Land
}
