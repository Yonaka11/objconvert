package com.mikazuki.pocketfamiliar.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.core.content.getSystemService
import com.mikazuki.pocketfamiliar.util.OverlayPermission

class PetOverlayManager(
    private val context: Context,
) {
    private val windowManager: WindowManager? = context.getSystemService()
    private var petView: PetOverlayView? = null

    val isShowing: Boolean
        get() = petView != null

    fun show(): Boolean {
        if (petView != null) {
            Log.d(TAG, "Pet overlay is already showing.")
            return true
        }
        if (!OverlayPermission.isGranted(context)) {
            Log.w(TAG, "Cannot show pet overlay because overlay permission is missing.")
            return false
        }
        val manager = windowManager ?: run {
            Log.e(TAG, "WindowManager is unavailable.")
            return false
        }

        val petSizePx = context.resources.displayMetrics.density.times(DEFAULT_PET_SIZE_DP).toInt()
        val params = WindowManager.LayoutParams(
            petSizePx,
            petSizePx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = initialX(petSizePx)
            y = initialY(petSizePx)
        }

        val view = PetOverlayView(context)
        return try {
            manager.addView(view, params)
            petView = view
            true
        } catch (exception: RuntimeException) {
            Log.e(TAG, "Failed to add pet overlay view.", exception)
            false
        }
    }

    fun remove() {
        val view = petView ?: return
        petView = null

        try {
            windowManager?.removeView(view)
        } catch (exception: IllegalArgumentException) {
            Log.w(TAG, "Pet overlay view was already removed.", exception)
        } catch (exception: RuntimeException) {
            Log.e(TAG, "Failed to remove pet overlay view.", exception)
        }
    }

    private fun initialX(petSizePx: Int): Int {
        val width = context.resources.displayMetrics.widthPixels
        val margin = context.resources.displayMetrics.density.times(INITIAL_MARGIN_DP).toInt()
        return (width - petSizePx - margin).coerceAtLeast(margin)
    }

    private fun initialY(petSizePx: Int): Int {
        val height = context.resources.displayMetrics.heightPixels
        val bottomOffset = context.resources.displayMetrics.density.times(INITIAL_BOTTOM_OFFSET_DP).toInt()
        val margin = context.resources.displayMetrics.density.times(INITIAL_MARGIN_DP).toInt()
        return (height - petSizePx - bottomOffset).coerceAtLeast(margin)
    }

    companion object {
        private const val TAG = "PetOverlayManager"
        private const val DEFAULT_PET_SIZE_DP = 96f
        private const val INITIAL_MARGIN_DP = 24f
        private const val INITIAL_BOTTOM_OFFSET_DP = 168f
    }
}
