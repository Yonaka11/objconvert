package com.mikazuki.pocketfamiliar.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.WindowInsets
import android.view.WindowManager
import androidx.core.content.ContextCompat
import com.mikazuki.pocketfamiliar.R
import com.mikazuki.pocketfamiliar.model.PetSettings
import com.mikazuki.pocketfamiliar.pet.PetState
import com.mikazuki.pocketfamiliar.pet.behavior.PetStateMachine
import com.mikazuki.pocketfamiliar.pet.physics.PetPhysicsEngine
import kotlinx.coroutines.CoroutineScope

private const val TAG = "PetOverlayManager"

/**
 * Manages adding, updating, and removing the pet [PetView] via [WindowManager].
 *
 * The overlay window is sized to the pet graphic only, so touches outside
 * the pet pass through to whatever app is underneath.
 */
class PetOverlayManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private var petView: PetView? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var isAdded = false

    private var physics: PetPhysicsEngine? = null
    private var stateMachine: PetStateMachine? = null

    // Finger offset so the pet doesn't jump to the touch point on drag start
    private var dragOffsetX = 0f
    private var dragOffsetY = 0f

    fun show(settings: PetSettings) {
        if (isAdded) {
            Log.w(TAG, "show() called while overlay is already visible")
            return
        }

        val petSizePx = dpToPx(settings.petSizeDp)
        val (screenW, screenH) = getUsableScreenBounds()

        val eng = PetPhysicsEngine(
            screenWidth = screenW,
            screenHeight = screenH,
            petWidth = petSizePx,
            petHeight = petSizePx
        ).apply {
            // Start centred horizontally, 40% down vertically
            x = ((screenW - petSizePx) / 2).toFloat()
            y = (screenH * 0.4f)
        }
        physics = eng

        val params = buildLayoutParams(petSizePx, eng.x.toInt(), eng.y.toInt())
        layoutParams = params

        val view = PetView(context).apply {
            petDrawable = ContextCompat.getDrawable(context, R.drawable.ic_pet_placeholder)
            dragListener = buildDragListener()
        }
        petView = view

        val machine = PetStateMachine(
            physics = eng,
            scope = scope,
            sleepEnabled = settings.sleepEnabled,
            speedMultiplier = settings.speedMultiplier,
            onStateChanged = { state -> onPetStateChanged(state) },
            onPositionChanged = { x, y -> updateWindowPosition(x.toInt(), y.toInt()) }
        )
        stateMachine = machine

        try {
            windowManager.addView(view, params)
            isAdded = true
            machine.start()
            Log.d(TAG, "Pet overlay added at (${eng.x}, ${eng.y}), size=${petSizePx}px")
        } catch (e: Exception) {
            Log.e(TAG, "WindowManager.addView failed", e)
            isAdded = false
        }
    }

    fun hide() {
        stateMachine?.stop()
        stateMachine = null

        val view = petView
        if (view != null && isAdded) {
            try {
                windowManager.removeView(view)
                Log.d(TAG, "Pet overlay removed")
            } catch (e: Exception) {
                Log.e(TAG, "WindowManager.removeView failed", e)
            } finally {
                isAdded = false
            }
        }
        petView = null
        layoutParams = null
        physics = null
    }

    /** Call when the device rotates or screen bounds change. */
    fun onScreenBoundsChanged() {
        val eng = physics ?: return
        val (w, h) = getUsableScreenBounds()
        eng.screenWidth = w
        eng.screenHeight = h
        eng.clampToScreenBounds()
        updateWindowPosition(eng.x.toInt(), eng.y.toInt())
        Log.d(TAG, "Screen bounds updated: ${w}x${h}")
    }

    private fun buildDragListener() = object : PetView.DragListener {
        override fun onDragStarted(rawX: Float, rawY: Float) {
            stateMachine?.onDragStarted()
            val params = layoutParams ?: return
            dragOffsetX = rawX - params.x
            dragOffsetY = rawY - params.y
        }

        override fun onDragMoved(rawX: Float, rawY: Float) {
            val eng = physics ?: return
            val newX = (rawX - dragOffsetX).toInt()
            val newY = (rawY - dragOffsetY).toInt()
            eng.x = newX.toFloat()
            eng.y = newY.toFloat()
            updateWindowPosition(newX, newY)
        }

        override fun onDragReleased(rawX: Float, rawY: Float) {
            stateMachine?.onDragReleased()
        }
    }

    private fun onPetStateChanged(state: PetState) {
        val view = petView ?: return
        // Mirror the sprite horizontally when walking left
        view.post {
            view.flipHorizontal = (state == PetState.WALK_LEFT)
        }
    }

    private fun updateWindowPosition(x: Int, y: Int) {
        val params = layoutParams ?: return
        val view = petView ?: return
        if (!isAdded) return

        params.x = x
        params.y = y
        try {
            windowManager.updateViewLayout(view, params)
        } catch (e: Exception) {
            Log.e(TAG, "updateViewLayout failed", e)
        }
    }

    private fun buildLayoutParams(sizePx: Int, startX: Int, startY: Int) =
        WindowManager.LayoutParams(
            sizePx,
            sizePx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = startX
            y = startY
        }

    /**
     * Returns screen width and usable height (excluding system bars).
     *
     * Uses the modern [WindowMetrics] API on API 30+ and falls back to the
     * deprecated [Display.getSize] on older versions.
     */
    @Suppress("DEPRECATION")
    private fun getUsableScreenBounds(): Pair<Int, Int> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val metrics = windowManager.currentWindowMetrics
            val insets = metrics.windowInsets.getInsetsIgnoringVisibility(
                WindowInsets.Type.systemBars()
            )
            Pair(
                metrics.bounds.width(),
                metrics.bounds.height() - insets.bottom - insets.top
            )
        } else {
            val display = windowManager.defaultDisplay
            val size = android.graphics.Point()
            display.getSize(size)
            Pair(size.x, size.y)
        }
    }

    private fun dpToPx(dp: Float): Int =
        (dp * context.resources.displayMetrics.density).toInt()
}
