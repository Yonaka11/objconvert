package com.mikazuki.pocketfamiliar.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

/**
 * The on-screen pet widget rendered via WindowManager.
 *
 * Why a plain View instead of ComposeView:
 * ComposeView inside WindowManager requires careful lifecycle owner and
 * ViewTreeSavedStateRegistryOwner plumbing that can break unpredictably
 * across Android versions. A plain View is stable, has minimal overhead,
 * and is straightforward to animate with a Handler-based loop.
 */
class PetView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    interface DragListener {
        fun onDragStarted(rawX: Float, rawY: Float)
        fun onDragMoved(rawX: Float, rawY: Float)
        fun onDragReleased(rawX: Float, rawY: Float)
    }

    var dragListener: DragListener? = null

    var petDrawable: Drawable? = null
        set(value) {
            field = value
            invalidate()
        }

    /** When true, the drawable is mirrored horizontally (walk-left uses walk-right art). */
    var flipHorizontal: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    private val flipMatrix = Matrix()
    private var isDragging = false

    override fun onDraw(canvas: Canvas) {
        val d = petDrawable ?: return
        d.setBounds(0, 0, width, height)

        if (flipHorizontal) {
            canvas.save()
            flipMatrix.setScale(-1f, 1f, width / 2f, height / 2f)
            canvas.concat(flipMatrix)
            d.draw(canvas)
            canvas.restore()
        } else {
            d.draw(canvas)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                isDragging = true
                dragListener?.onDragStarted(event.rawX, event.rawY)
                true
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    dragListener?.onDragMoved(event.rawX, event.rawY)
                }
                true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDragging) {
                    isDragging = false
                    dragListener?.onDragReleased(event.rawX, event.rawY)
                }
                true
            }
            else -> false
        }
    }
}
