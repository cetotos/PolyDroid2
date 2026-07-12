package com.cetotos.polydroid2

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View

class CustomKeysOverlay(
    context: Context,
    private val renderWidth: Int,
    private val renderHeight: Int,
    private val sendKey: (scanCode: Int, down: Boolean) -> Unit,
    private val sendTouch: (type: Int, id: Int, x: Int, y: Int) -> Unit,
) : View(context) {

    companion object {
        private const val TOUCH_BEGIN = 10
        private const val TOUCH_UPDATE = 11
        private const val TOUCH_END = 12
    }

    private class KeyButton(val ck: SettingsActivity.CustomKey, density: Float) {
        val radius = TouchControlOverlay.CUSTOM_RADIUS_DP * density * ck.scale
        var cx = 0f
        var cy = 0f
        var pointerId = -1
        var toggleActive = false
    }

    private val density = context.resources.displayMetrics.density
    private val buttons = SettingsActivity.getCustomKeys(context, v2 = true).map { KeyButton(it, density) }

    private val fillPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val strokePaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
        isAntiAlias = true
    }
    private val labelPaint = Paint().apply {
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    private val shapeRect = RectF()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        for (b in buttons) {
            b.cx = b.ck.xFrac * w
            b.cy = b.ck.yFrac * h
        }
    }

    private fun mx(x: Float): Int {
        val w = if (width > 0) width else 1
        return (x / w * renderWidth).toInt().coerceIn(0, renderWidth - 1)
    }

    private fun my(y: Float): Int {
        val h = if (height > 0) height else 1
        return (y / h * renderHeight).toInt().coerceIn(0, renderHeight - 1)
    }

    private fun tryPress(pointerId: Int, x: Float, y: Float): Boolean {
        for (b in buttons) {
            val hit = kotlin.math.abs(x - b.cx) <= b.radius && kotlin.math.abs(y - b.cy) <= b.radius
            if (!hit || b.pointerId != -1) continue
            b.pointerId = pointerId
            if (b.ck.toggle) {
                b.toggleActive = !b.toggleActive
                sendKey(b.ck.scanCode, b.toggleActive)
            } else {
                sendKey(b.ck.scanCode, true)
            }
            invalidate()
            return true
        }
        return false
    }

    private fun release(pointerId: Int): Boolean {
        val b = buttons.firstOrNull { it.pointerId == pointerId } ?: return false
        b.pointerId = -1
        if (!b.ck.toggle) sendKey(b.ck.scanCode, false)
        invalidate()
        return true
    }

    private fun owns(pointerId: Int): Boolean = buttons.any { it.pointerId == pointerId }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val pi = event.actionIndex
                val id = event.getPointerId(pi)
                if (!tryPress(id, event.getX(pi), event.getY(pi))) {
                    sendTouch(TOUCH_BEGIN, id, mx(event.getX(pi)), my(event.getY(pi)))
                }
            }
            MotionEvent.ACTION_MOVE -> {
                for (pi in 0 until event.pointerCount) {
                    val id = event.getPointerId(pi)
                    if (owns(id)) continue
                    sendTouch(TOUCH_UPDATE, id, mx(event.getX(pi)), my(event.getY(pi)))
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                val pi = event.actionIndex
                val id = event.getPointerId(pi)
                if (!release(id)) {
                    sendTouch(TOUCH_END, id, mx(event.getX(pi)), my(event.getY(pi)))
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                for (pi in 0 until event.pointerCount) {
                    val id = event.getPointerId(pi)
                    if (release(id)) continue
                    sendTouch(TOUCH_END, id, mx(event.getX(pi)), my(event.getY(pi)))
                }
            }
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (b in buttons) {
            val active = if (b.ck.toggle) b.toggleActive else b.pointerId != -1
            val cr = Color.red(b.ck.color); val cg = Color.green(b.ck.color); val cb = Color.blue(b.ck.color)
            val fillAlpha = ((if (active) 120 else 60) * b.ck.opacity).toInt()
            fillPaint.color = Color.argb(fillAlpha, cr, cg, cb)
            strokePaint.color = Color.argb((160 * b.ck.opacity).toInt().coerceAtLeast(30), cr, cg, cb)
            if (b.ck.shape == SettingsActivity.SHAPE_SQUARE) {
                shapeRect.set(b.cx - b.radius, b.cy - b.radius, b.cx + b.radius, b.cy + b.radius)
                val corner = b.radius * 0.25f
                canvas.drawRoundRect(shapeRect, corner, corner, fillPaint)
                canvas.drawRoundRect(shapeRect, corner, corner, strokePaint)
            } else {
                canvas.drawCircle(b.cx, b.cy, b.radius, fillPaint)
                canvas.drawCircle(b.cx, b.cy, b.radius, strokePaint)
            }
            if (b.ck.label.isNotEmpty()) {
                labelPaint.color = Color.argb((220 * b.ck.opacity).toInt().coerceAtLeast(50), 255, 255, 255)
                labelPaint.textSize = b.radius * 0.7f
                val w = labelPaint.measureText(b.ck.label)
                val maxW = b.radius * 1.8f
                if (w > maxW) labelPaint.textSize *= maxW / w
                labelPaint.textSize *= b.ck.textScale
                canvas.drawText(b.ck.label, b.cx, b.cy + labelPaint.textSize / 3f, labelPaint)
            }
        }
    }
}
