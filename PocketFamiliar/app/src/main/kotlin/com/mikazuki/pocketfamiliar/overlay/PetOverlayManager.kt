package com.mikazuki.pocketfamiliar.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import com.mikazuki.pocketfamiliar.pet.behavior.PetState
import kotlin.math.roundToInt

private const val TAG = "PetOverlayManager"

/**
 * Owns the [PetView] window and exposes position/state update entry-points
 * that the service calls on every tick.
 *
 * Window parameters:
 *  - TYPE_APPLICATION_OVERLAY — appears over other apps (requires SYSTEM_ALERT_WINDOW)
 *  - FLAG_NOT_FOCUSABLE        — key events pass through to the app beneath
 *  - FLAG_NOT_TOUCH_MODAL      — touches outside the view pass through
 *  - FLAG_LAYOUT_IN_SCREEN     — coordinates are relative to the full screen
 *
 * The overlay is sized to the pet (e.g. 128 × 128 dp), not the full screen, so
 * it never blocks an invisible touch region.
 */
class PetOverlayManager(
    private val context: Context,
    private val onDragStarted: () -> Unit,
    private val onDragReleased: (releaseVelocityY: Float) -> Unit,
) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var petView: PetView? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var isAdded = false

    // Drag tracking
    private var dragOffsetX = 0f
    private var dragOffsetY = 0f
    private var lastTouchY = 0f
    private var lastTouchTimeMs = 0L
    private var dragVelocityY = 0f

    val petSizePx: Int get() = layoutParams?.width ?: 128

    fun create(petSizeDp: Float) {
        if (isAdded) {
            Log.w(TAG, "Overlay already added — skipping create()")
            return
        }

        val density = context.resources.displayMetrics.density
        val sizePx = (petSizeDp * 64 * density).roundToInt().coerceAtLeast(48)

        val params = WindowManager.LayoutParams(
            sizePx,
            sizePx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = getScreenWidth() / 2 - sizePx / 2
            y = getScreenHeight() / 4
        }

        val view = PetView(context)
        view.setOnTouchListener { _, event -> handleTouch(event, params) }

        try {
            windowManager.addView(view, params)
            petView = view
            layoutParams = params
            isAdded = true
            Log.d(TAG, "Overlay added at (${params.x}, ${params.y}), size=$sizePx")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add overlay view", e)
        }
    }

    fun remove() {
        val view = petView ?: return
        if (!isAdded) return
        try {
            windowManager.removeView(view)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove overlay view", e)
        } finally {
            petView = null
            layoutParams = null
            isAdded = false
        }
    }

    fun updatePosition(x: Float, y: Float) {
        val params = layoutParams ?: return
        val view = petView ?: return
        if (!isAdded) return

        params.x = x.roundToInt()
        params.y = y.roundToInt()
        try {
            windowManager.updateViewLayout(view, params)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update overlay position", e)
        }
    }

    fun applyState(state: PetState) {
        petView?.applyState(state)
    }

    fun tick() {
        petView?.tick()
    }

    /** Returns the current (x, y) position of the pet window as set during drag. */
    fun getDragPosition(): Pair<Float, Float> {
        val params = layoutParams ?: return Pair(0f, 0f)
        return Pair(params.x.toFloat(), params.y.toFloat())
    }

    fun updatePetSize(petSizeDp: Float) {
        val params = layoutParams ?: return
        val view = petView ?: return
        if (!isAdded) return

        val density = context.resources.displayMetrics.density
        val sizePx = (petSizeDp * 64 * density).roundToInt().coerceAtLeast(48)
        params.width = sizePx
        params.height = sizePx
        try {
            windowManager.updateViewLayout(view, params)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resize overlay", e)
        }
    }

    fun getScreenWidth(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowManager.currentWindowMetrics.bounds.width()
        } else {
            @Suppress("DEPRECATION")
            val size = Point()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getSize(size)
            size.x
        }
    }

    fun getScreenHeight(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowManager.currentWindowMetrics.bounds.height()
        } else {
            @Suppress("DEPRECATION")
            val size = Point()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getSize(size)
            size.y
        }
    }

    private fun handleTouch(event: MotionEvent, params: WindowManager.LayoutParams): Boolean {
        return when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                dragOffsetX = event.rawX - params.x
                dragOffsetY = event.rawY - params.y
                lastTouchY = event.rawY
                lastTouchTimeMs = System.currentTimeMillis()
                dragVelocityY = 0f
                onDragStarted()
                true
            }
            MotionEvent.ACTION_MOVE -> {
                val newX = event.rawX - dragOffsetX
                val newY = event.rawY - dragOffsetY

                // Track Y velocity for realistic post-release fall
                val now = System.currentTimeMillis()
                val dt = (now - lastTouchTimeMs).coerceAtLeast(1L) / 1000f
                dragVelocityY = (event.rawY - lastTouchY) / dt
                lastTouchY = event.rawY
                lastTouchTimeMs = now

                params.x = newX.roundToInt()
                params.y = newY.roundToInt()
                try {
                    windowManager.updateViewLayout(petView, params)
                } catch (e: Exception) {
                    Log.e(TAG, "Move update failed", e)
                }
                true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                onDragReleased(dragVelocityY.coerceIn(0f, 1800f))
                true
            }
            else -> false
        }
    }
}
