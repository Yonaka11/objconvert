package com.mikazuki.pocketfamiliar.pet.behavior

import android.util.Log
import com.mikazuki.pocketfamiliar.pet.PetState
import com.mikazuki.pocketfamiliar.pet.physics.PetPhysicsEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

private const val TAG = "PetController"

/**
 * State machine that decides what the pet should be doing at any moment.
 *
 * Rules (MVP):
 *  - IDLE → after a random delay → WALK_LEFT or WALK_RIGHT
 *  - IDLE → occasionally → SLEEP (if sleepEnabled)
 *  - WALK → after a random duration → IDLE
 *  - WALK → hits left edge → WALK_RIGHT
 *  - WALK → hits right edge → WALK_LEFT
 *  - Any state → user grabs → DRAGGED (set externally via [onDragStart])
 *  - DRAGGED → released → FALLING (set externally via [onDragEnd])
 *  - FALLING → lands on floor → IDLE
 *
 * The autonomous behavior loop runs as a coroutine inside [scope].
 * [physics] is queried for edge collisions; position updates are owned by the overlay loop.
 */
class PetController(
    private val scope: CoroutineScope,
    private val physics: PetPhysicsEngine,
    private var sleepEnabled: Boolean = true,
) {
    private val _state = MutableStateFlow<PetState>(PetState.IDLE)
    val state: StateFlow<PetState> = _state.asStateFlow()

    private var behaviorJob: Job? = null

    fun start() {
        behaviorJob?.cancel()
        behaviorJob = scope.launch { runBehaviorLoop() }
    }

    fun stop() {
        behaviorJob?.cancel()
        behaviorJob = null
    }

    fun setSleepEnabled(enabled: Boolean) {
        sleepEnabled = enabled
    }

    // ── External triggers ─────────────────────────────────────────────────

    fun onDragStart() {
        behaviorJob?.cancel()
        _state.value = PetState.DRAGGED
    }

    fun onDragEnd() {
        if (_state.value == PetState.DRAGGED) {
            _state.value = PetState.FALLING
            behaviorJob = scope.launch { waitForLanding() }
        }
    }

    // ── Called each physics tick by the overlay loop ──────────────────────

    /**
     * React to physics-detected edge conditions.
     * Must be called after [PetPhysicsEngine.update] each frame.
     */
    fun onPhysicsUpdate() {
        when (_state.value) {
            PetState.WALK_LEFT -> {
                if (physics.isAtLeftEdge()) {
                    Log.d(TAG, "Hit left edge → WALK_RIGHT")
                    _state.value = PetState.WALK_RIGHT
                }
            }
            PetState.WALK_RIGHT -> {
                if (physics.isAtRightEdge()) {
                    Log.d(TAG, "Hit right edge → WALK_LEFT")
                    _state.value = PetState.WALK_LEFT
                }
            }
            PetState.FALLING -> {
                if (physics.isOnFloor()) {
                    Log.d(TAG, "Landed → IDLE")
                    physics.snapToFloor()
                    _state.value = PetState.IDLE
                    behaviorJob?.cancel()
                    behaviorJob = scope.launch { runBehaviorLoop() }
                }
            }
            else -> {}
        }
    }

    // ── Internal behavior loop ────────────────────────────────────────────

    private suspend fun runBehaviorLoop() {
        while (true) {
            val current = _state.value
            if (current.isPlayerControlled || current == PetState.FALLING) return

            val idleDelay = Random.nextLong(2_000L, 6_000L)
            delay(idleDelay)

            if (_state.value.isPlayerControlled) return

            // Occasionally go to sleep instead of walking
            val roll = Random.nextFloat()
            val nextState = when {
                sleepEnabled && roll < 0.15f -> PetState.SLEEP
                roll < 0.55f -> PetState.WALK_LEFT
                else -> PetState.WALK_RIGHT
            }
            Log.d(TAG, "IDLE → $nextState")
            _state.value = nextState

            if (nextState == PetState.SLEEP) {
                val sleepDuration = Random.nextLong(5_000L, 15_000L)
                delay(sleepDuration)
                if (!_state.value.isPlayerControlled) {
                    Log.d(TAG, "SLEEP → IDLE")
                    _state.value = PetState.IDLE
                }
            } else {
                val walkDuration = Random.nextLong(1_500L, 5_000L)
                delay(walkDuration)
                if (_state.value.isWalking) {
                    Log.d(TAG, "${_state.value} → IDLE")
                    _state.value = PetState.IDLE
                }
            }
        }
    }

    private suspend fun waitForLanding() {
        // Poll: landing detection happens in onPhysicsUpdate.
        // This coroutine is a safety timeout in case physics is paused.
        delay(10_000L)
        if (_state.value == PetState.FALLING) {
            Log.w(TAG, "Landing timeout; snapping to floor")
            physics.snapToFloor()
            _state.value = PetState.IDLE
            runBehaviorLoop()
        }
    }
}
