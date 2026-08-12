package com.mikazuki.pocketfamiliar.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.ImageView
import com.mikazuki.pocketfamiliar.pet.PetState
import com.mikazuki.pocketfamiliar.pet.animation.PetAnimation
import com.mikazuki.pocketfamiliar.pet.animation.PetAnimations
import com.mikazuki.pocketfamiliar.pet.behavior.PetController
import com.mikazuki.pocketfamiliar.pet.physics.PetPhysicsEngine
import kotlin.math.roundToInt

private const val TAG = "PetOverlayManager"

/**
 * Manages the pet's [ImageView] window added directly to [WindowManager].
 *
 * ### Why ImageView (not ComposeView)?
 * Embedding a ComposeView into a WindowManager window requires a running
 * Compose host/lifecycle that is non-trivial to wire correctly inside a
 * foreground service — especially across configuration changes. A plain
 * [ImageView] driven by a coroutine loop is simpler, has no lifecycle
 * dependencies, and is easier to verify correct cleanup.
 * The settings Activity uses Compose exclusively.
 *
 * ### Overlay flags
 * - [WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE]: never steals keyboard focus
 * - [WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL]: passes touches outside
 *   the view to the underlying app
 * - The view is sized to the pet drawable only, so the untouchable region is
 *   exactly the area outside the pet graphic.
 */
class PetOverlayManager(
    private val context: Context,
    private val windowManager: WindowManager,
    private val physics: PetPhysicsEngine,
    private val controller: PetController,
) {
    private var petView: ImageView? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    private var isAdded = false
    private var animationStartMs = System.currentTimeMillis()
    private var currentAnimation: PetAnimation = PetAnimations.idle

    // Drag state
    private var dragOffsetX = 0f
    private var dragOffsetY = 0f
    private var lastRawX = 0f
    private var lastRawY = 0f

    /**
     * Creates the ImageView and adds it to the window.
     * Safe to call multiple times; subsequent calls are no-ops if already added.
     */
    fun attach() {
        if (isAdded) {
            Log.w(TAG, "attach() called but overlay is already added")
            return
        }

        val view = ImageView(context).apply {
            setImageResource(com.mikazuki.pocketfamiliar.R.drawable.ic_pet_placeholder)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setOnTouchListener { _, event -> handleTouch(event) }
        }

        val params = buildLayoutParams()
        petView = view
        layoutParams = params

        try {
            windowManager.addView(view, params)
            isAdded = true
            Log.d(TAG, "Pet overlay attached at (${params.x}, ${params.y})")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add pet view to WindowManager", e)
            petView = null
            layoutParams = null
        }
    }

    /**
     * Removes the view from the window. Safe to call when not attached.
     * Always call this when the service stops to prevent a WindowManager leak.
     */
    fun detach() {
        val view = petView ?: return
        try {
            if (isAdded) {
                windowManager.removeView(view)
                Log.d(TAG, "Pet overlay detached")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing pet view", e)
        } finally {
            petView = null
            layoutParams = null
            isAdded = false
        }
    }

    /**
     * Called every animation/physics frame by the service's coroutine loop.
     * Updates the overlay position from [physics] and advances sprite animation.
     *
     * @param state        Current pet state (drives animation selection).
     * @param elapsedMs    Time in milliseconds since the service started (for animation timing).
     */
    fun onFrame(state: PetState, elapsedMs: Long) {
        if (!isAdded) return
        val view = petView ?: return
        val params = layoutParams ?: return

        // Switch animation when state changes
        val targetAnimation = PetAnimations.forState(state)
        if (targetAnimation != currentAnimation) {
            currentAnimation = targetAnimation
            animationStartMs = System.currentTimeMillis()
        }

        // Advance sprite frame
        val frameElapsed = System.currentTimeMillis() - animationStartMs
        val frameResId = currentAnimation.frameAt(frameElapsed)
        view.setImageResource(frameResId)

        // Apply horizontal flip for left-facing animations
        val scaleX = if (currentAnimation.flipHorizontal) -1f else 1f
        if (view.scaleX != scaleX) {
            view.scaleX = scaleX
        }

        // Sync position from physics
        val newX = physics.x.roundToInt()
        val newY = physics.y.roundToInt()
        if (params.x != newX || params.y != newY) {
            params.x = newX
            params.y = newY
            try {
                windowManager.updateViewLayout(view, params)
            } catch (e: Exception) {
                Log.e(TAG, "updateViewLayout failed", e)
            }
        }
    }

    /**
     * Updates pet size. Pass a scale factor (1.0 = default 64×80dp).
     */
    fun updateSize(scaleFactor: Float) {
        val baseDp = 64
        val baseHeightDp = 80
        val density = context.resources.displayMetrics.density
        val newWidth = (baseDp * density * scaleFactor).roundToInt()
        val newHeight = (baseHeightDp * density * scaleFactor).roundToInt()

        val params = layoutParams ?: return
        val view = petView ?: return
        if (params.width == newWidth && params.height == newHeight) return

        params.width = newWidth
        params.height = newHeight
        physics.petWidth = newWidth
        physics.petHeight = newHeight

        try {
            windowManager.updateViewLayout(view, params)
        } catch (e: Exception) {
            Log.e(TAG, "updateViewLayout (size) failed", e)
        }
    }

    /**
     * Refreshes stored screen dimensions from the WindowManager.
     * Call this after rotation or other display changes.
     */
    fun updateScreenBounds() {
        val bounds = getUsableScreenBounds()
        physics.screenWidth = bounds.width()
        physics.screenHeight = bounds.height()
        physics.clampToScreen()
        Log.d(TAG, "Screen bounds updated: ${bounds.width()}×${bounds.height()}")
    }

    // ── Touch handling ────────────────────────────────────────────────────

    private fun handleTouch(event: MotionEvent): Boolean {
        return when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                dragOffsetX = event.rawX - physics.x
                dragOffsetY = event.rawY - physics.y
                lastRawX = event.rawX
                lastRawY = event.rawY
                controller.onDragStart()
                true
            }
            MotionEvent.ACTION_MOVE -> {
                lastRawX = event.rawX
                lastRawY = event.rawY
                physics.setPosition(
                    event.rawX - dragOffsetX,
                    event.rawY - dragOffsetY,
                )
                // Immediately sync position while dragging
                val params = layoutParams ?: return true
                val view = petView ?: return true
                params.x = physics.x.roundToInt()
                params.y = physics.y.roundToInt()
                try {
                    windowManager.updateViewLayout(view, params)
                } catch (e: Exception) {
                    Log.e(TAG, "updateViewLayout (drag) failed", e)
                }
                true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                controller.onDragEnd()
                true
            }
            else -> false
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private fun buildLayoutParams(): WindowManager.LayoutParams {
        val density = context.resources.displayMetrics.density
        val petWidthPx = (64 * density).roundToInt()
        val petHeightPx = (80 * density).roundToInt()

        physics.petWidth = petWidthPx
        physics.petHeight = petHeightPx

        val bounds = getUsableScreenBounds()
        physics.screenWidth = bounds.width()
        physics.screenHeight = bounds.height()

        // Start near top-center of the screen
        physics.x = ((bounds.width() - petWidthPx) / 2).toFloat()
        physics.y = (bounds.height() * 0.15f)

        return WindowManager.LayoutParams(
            petWidthPx,
            petHeightPx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = physics.x.roundToInt()
            y = physics.y.roundToInt()
        }
    }

    @Suppress("DEPRECATION")
    private fun getUsableScreenBounds(): Rect {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val metrics = windowManager.currentWindowMetrics
            val insets = metrics.windowInsets.getInsetsIgnoringVisibility(
                WindowInsets.Type.systemBars()
            )
            val bounds = metrics.bounds
            Rect(
                bounds.left + insets.left,
                bounds.top + insets.top,
                bounds.right - insets.right,
                bounds.bottom - insets.bottom,
            )
        } else {
            val display = windowManager.defaultDisplay
            val size = android.graphics.Point()
            display.getSize(size)
            Rect(0, 0, size.x, size.y)
        }
    }
}
