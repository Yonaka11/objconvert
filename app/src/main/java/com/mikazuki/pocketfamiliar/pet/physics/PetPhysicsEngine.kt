package com.mikazuki.pocketfamiliar.pet.physics

import kotlin.math.min

/**
 * Lightweight physics engine for the pet overlay.
 *
 * Uses delta-time arithmetic so behavior is frame-rate independent.
 * All units are pixels; coordinates use top-left origin matching WindowManager params.
 */
class PetPhysicsEngine(
    var screenWidth: Int = 0,
    var screenHeight: Int = 0,
    var petWidth: Int = 0,
    var petHeight: Int = 0
) {
    var x: Float = 0f
    var y: Float = 0f
    var velocityX: Float = 0f
    var velocityY: Float = 0f

    companion object {
        private const val GRAVITY = 1800f        // px/s²
        private const val MAX_FALL_SPEED = 3000f // px/s terminal velocity
    }

    /**
     * Advances gravity-driven fall by [deltaSeconds].
     * Returns true when the pet has landed on the bottom boundary.
     */
    fun applyGravityStep(deltaSeconds: Float): Boolean {
        velocityY = min(velocityY + GRAVITY * deltaSeconds, MAX_FALL_SPEED)
        y += velocityY * deltaSeconds

        val bottomBound = (screenHeight - petHeight).toFloat()
        if (y >= bottomBound) {
            y = bottomBound
            velocityY = 0f
            velocityX = 0f
            return true
        }
        return false
    }

    /**
     * Advances horizontal walk by [deltaSeconds] at [speedPxPerSecond].
     * Positive speed → right; negative speed → left.
     * Returns which screen edge was hit, if any.
     */
    fun applyWalkStep(deltaSeconds: Float, speedPxPerSecond: Float): EdgeCollision {
        x += speedPxPerSecond * deltaSeconds
        return clampToScreenBounds()
    }

    /**
     * Clamps position to screen boundaries and returns which edge (if any) was hit.
     */
    fun clampToScreenBounds(): EdgeCollision {
        var collision = EdgeCollision.NONE

        val rightBound = (screenWidth - petWidth).toFloat()
        when {
            x <= 0f -> {
                x = 0f
                collision = EdgeCollision.LEFT
            }
            x >= rightBound -> {
                x = rightBound
                collision = EdgeCollision.RIGHT
            }
        }

        if (y < 0f) y = 0f
        val bottomBound = (screenHeight - petHeight).toFloat()
        if (y > bottomBound) y = bottomBound

        return collision
    }

    fun reset(startX: Float, startY: Float) {
        x = startX
        y = startY
        velocityX = 0f
        velocityY = 0f
    }
}

enum class EdgeCollision { NONE, LEFT, RIGHT, TOP, BOTTOM }
