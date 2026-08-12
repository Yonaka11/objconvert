package com.mikazuki.pocketfamiliar.pet.physics

import com.mikazuki.pocketfamiliar.pet.PetState

/**
 * Lightweight, frame-rate-independent physics for the pet overlay.
 *
 * The engine only stores mutable state (position, velocity, dimensions).
 * The caller drives it by calling [update] with a delta-time value each frame.
 *
 * All coordinates are in pixels relative to the top-left of the screen's
 * usable area (i.e. excluding status bar and navigation bar).
 */
class PetPhysicsEngine {

    // ── Screen bounds (updated on rotation / window change) ───────────────
    var screenWidth: Int = 1080
    var screenHeight: Int = 1920
    var petWidth: Int = 128
    var petHeight: Int = 160

    // ── Current position (top-left corner of the pet window) ──────────────
    var x: Float = 100f
    var y: Float = 100f

    // ── Velocity (px / second) ────────────────────────────────────────────
    var velocityX: Float = 0f
    var velocityY: Float = 0f

    // ── Gravity (px / second²) ────────────────────────────────────────────
    var gravity: Float = 1800f

    // ── Walking speed base (px / second); scaled by movementSpeed setting ─
    var walkSpeed: Float = 120f

    /** Maximum y velocity to prevent the pet from tunnelling through the floor. */
    private val terminalVelocity = 2400f

    /**
     * Advances physics by [deltaSeconds] seconds.
     * Only applies physics appropriate to the current [state].
     */
    fun update(state: PetState, deltaSeconds: Float) {
        when (state) {
            PetState.FALLING -> applyFalling(deltaSeconds)
            PetState.WALK_LEFT -> applyWalking(-walkSpeed, deltaSeconds)
            PetState.WALK_RIGHT -> applyWalking(walkSpeed, deltaSeconds)
            PetState.DRAGGED -> {} // Position is set externally by touch handler
            else -> {
                velocityX = 0f
                velocityY = 0f
            }
        }
        clampToScreen()
    }

    private fun applyFalling(dt: Float) {
        velocityY = (velocityY + gravity * dt).coerceAtMost(terminalVelocity)
        y += velocityY * dt
    }

    private fun applyWalking(speed: Float, dt: Float) {
        velocityX = speed
        velocityY = 0f
        x += velocityX * dt
    }

    /**
     * Returns true if the pet has landed on the floor (bottom boundary).
     * The caller should transition to IDLE when this is true during FALLING.
     */
    fun isOnFloor(): Boolean = y >= maxY().toFloat()

    /**
     * Returns true if the pet is touching the left wall.
     * The caller should flip direction (WALK_LEFT → WALK_RIGHT) when walking.
     */
    fun isAtLeftEdge(): Boolean = x <= 0f

    /**
     * Returns true if the pet is touching the right wall.
     * The caller should flip direction (WALK_RIGHT → WALK_LEFT) when walking.
     */
    fun isAtRightEdge(): Boolean = x >= maxX().toFloat()

    /** Snaps the pet to the floor surface. Called when landing from FALLING. */
    fun snapToFloor() {
        y = maxY().toFloat()
        velocityY = 0f
        velocityX = 0f
    }

    /** Teleports the pet to a specific screen position, clamped to valid bounds. */
    fun setPosition(px: Float, py: Float) {
        x = px.coerceIn(0f, maxX().toFloat())
        y = py.coerceIn(0f, maxY().toFloat())
    }

    /** Clamps current position so the pet never leaves the visible area. */
    fun clampToScreen() {
        x = x.coerceIn(0f, maxX().toFloat())
        y = y.coerceIn(0f, maxY().toFloat())
    }

    private fun maxX() = (screenWidth - petWidth).coerceAtLeast(0)
    private fun maxY() = (screenHeight - petHeight).coerceAtLeast(0)
}
