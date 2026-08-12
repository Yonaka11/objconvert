package com.mikazuki.pocketfamiliar.pet.behavior

import android.util.Log
import com.mikazuki.pocketfamiliar.pet.PetState
import com.mikazuki.pocketfamiliar.pet.physics.EdgeCollision
import com.mikazuki.pocketfamiliar.pet.physics.PetPhysicsEngine
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.random.Random

private const val TAG = "PetStateMachine"

// Walk speed in pixels per second (base, before multiplier)
private const val BASE_WALK_SPEED_PX = 120f
private const val TICK_MS = 16L // ~60fps physics tick

/**
 * Controls autonomous pet behavior via a state machine.
 *
 * The machine runs two coroutine loops:
 *  - [scheduleBehaviorLoop]: decides what the pet does next (idle, walk, sleep)
 *  - [physicsLoop]: applies gravity when the pet is FALLING
 *
 * Both loops are cancelled when the user grabs the pet and restarted on release.
 */
class PetStateMachine(
    private val physics: PetPhysicsEngine,
    private val scope: CoroutineScope,
    private val onStateChanged: (PetState) -> Unit,
    private val onPositionChanged: (x: Float, y: Float) -> Unit,
    var sleepEnabled: Boolean = true,
    var speedMultiplier: Float = 1.0f
) {
    private val _state = MutableStateFlow(PetState.IDLE)
    val state: StateFlow<PetState> = _state

    private var behaviorJob: Job? = null
    private var physicsJob: Job? = null

    fun start() {
        scheduleBehaviorLoop()
    }

    fun stop() {
        behaviorJob?.cancel()
        physicsJob?.cancel()
    }

    fun onDragStarted() {
        behaviorJob?.cancel()
        physicsJob?.cancel()
        transitionTo(PetState.DRAGGED)
    }

    fun onDragReleased() {
        transitionTo(PetState.FALLING)
        startPhysicsLoop()
    }

    private fun transitionTo(newState: PetState) {
        if (_state.value != newState) {
            Log.d(TAG, "State: ${_state.value} → $newState")
            _state.value = newState
            onStateChanged(newState)
        }
    }

    private fun scheduleBehaviorLoop() {
        behaviorJob?.cancel()
        behaviorJob = scope.launch {
            while (isActive) {
                // Idle pause before choosing the next action
                delay(Random.nextLong(1500L, 4000L))

                if (!isActive) break
                val current = _state.value
                if (current == PetState.DRAGGED || current == PetState.FALLING) continue

                val nextState = pickNextState()
                transitionTo(nextState)

                when (nextState) {
                    PetState.SLEEP -> {
                        delay(Random.nextLong(3000L, 9000L))
                    }
                    PetState.WALK_LEFT, PetState.WALK_RIGHT -> {
                        executeWalk(nextState)
                    }
                    else -> { /* IDLE — just wait for the loop to pick again */ }
                }

                if (_state.value != PetState.DRAGGED && _state.value != PetState.FALLING) {
                    transitionTo(PetState.IDLE)
                }
            }
        }
    }

    private fun pickNextState(): PetState {
        val roll = Random.nextInt(10)
        return when {
            sleepEnabled && roll == 0 -> PetState.SLEEP
            roll <= 4 -> PetState.WALK_LEFT
            roll <= 8 -> PetState.WALK_RIGHT
            else -> PetState.IDLE
        }
    }

    private suspend fun executeWalk(direction: PetState) {
        val sign = if (direction == PetState.WALK_LEFT) -1f else 1f
        val speed = BASE_WALK_SPEED_PX * speedMultiplier * sign
        val walkDurationMs = Random.nextLong(1500L, 5000L)
        val endTime = System.currentTimeMillis() + walkDurationMs

        var lastTick = System.currentTimeMillis()
        while (System.currentTimeMillis() < endTime && isActive()) {
            if (_state.value == PetState.DRAGGED || _state.value == PetState.FALLING) return

            val now = System.currentTimeMillis()
            val delta = (now - lastTick) / 1000f
            lastTick = now

            val collision = physics.applyWalkStep(delta, speed)
            onPositionChanged(physics.x, physics.y)

            when (collision) {
                EdgeCollision.LEFT -> {
                    transitionTo(PetState.WALK_RIGHT)
                    executeWalk(PetState.WALK_RIGHT)
                    return
                }
                EdgeCollision.RIGHT -> {
                    transitionTo(PetState.WALK_LEFT)
                    executeWalk(PetState.WALK_LEFT)
                    return
                }
                else -> {}
            }

            delay(TICK_MS)
        }
    }

    private fun startPhysicsLoop() {
        physicsJob?.cancel()
        physicsJob = scope.launch {
            var lastTick = System.currentTimeMillis()
            while (isActive && _state.value == PetState.FALLING) {
                val now = System.currentTimeMillis()
                val delta = (now - lastTick) / 1000f
                lastTick = now

                val landed = physics.applyGravityStep(delta)
                onPositionChanged(physics.x, physics.y)

                if (landed) {
                    transitionTo(PetState.IDLE)
                    scheduleBehaviorLoop()
                    return@launch
                }
                delay(TICK_MS)
            }
        }
    }

    private fun isActive(): Boolean = behaviorJob?.isActive == true
}
