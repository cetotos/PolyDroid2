package com.cetotos.polydroid2

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.MotionEvent
import android.view.View
import android.graphics.RectF
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.min

@SuppressLint("ViewConstructor")
class TouchControlOverlay(
    context: Context,
    private val renderWidth: Int,
    private val renderHeight: Int,
    private val sendInput: (type: Int, button: Int, x: Int, y: Int) -> Unit,
    private val sendKey: (scanCode: Int, keyCode: Int, down: Boolean) -> Unit,
) : View(context) {

    companion object {
        private const val JOYSTICK_RADIUS_DP = 75f
        private const val JOYSTICK_KNOB_RADIUS_DP = 33f
        private const val JOYSTICK_DEAD_ZONE = 0.15f
        private const val JOYSTICK_MARGIN_DP = 40f
        private const val JOYSTICK_HITBOX_PADDING_DP = 20f

        private const val TAP_MAX_MS = 250L
        private const val TAP_MAX_PX = 25f

        private const val SCAN_W = 17
        private const val SCAN_A = 30
        private const val SCAN_S = 31
        private const val SCAN_D = 32
        private const val SCAN_SPACE = 57
        private const val INPUT_BUTTON_DOWN = 2
        private const val INPUT_BUTTON_UP = 3
        private const val INPUT_MOTION = 1
        private const val MOUSE_LEFT = 1
        private const val MOUSE_RIGHT = 3
    }

    private val density = context.resources.displayMetrics.density
    private val joystickRadius = JOYSTICK_RADIUS_DP * density
    private val joystickKnobRadius = JOYSTICK_KNOB_RADIUS_DP * density

    private var joystickPointerId = -1
    private var joystickCenterX = 0f
    private var joystickCenterY = 0f
    private var joystickKnobX = 0f
    private var joystickKnobY = 0f
    private var joystickActive = false

    private var keyWDown = false
    private var keyADown = false
    private var keySDown = false
    private var keyDDown = false
    private var cameraPointerId = -1
    private var cameraStartX = 0f
    private var cameraStartY = 0f
    private var cameraLastX = 0f
    private var cameraLastY = 0f
    private var cameraStartTime = 0L
    private var cameraDragging = false
    private var cameraMoved = false
    private var cameraDeltaX = 0f
    private var cameraDeltaY = 0f

    private var jumpPointerId = -1

    private val joystickBasePaint = Paint().apply {
        color = Color.argb(60, 255, 255, 255)
        style = Paint.Style.FILL
    }
    private val joystickKnobPaint = Paint().apply {
        color = Color.argb(140, 255, 255, 255)
        style = Paint.Style.FILL
    }
    private val joystickOutlinePaint = Paint().apply {
        color = Color.argb(80, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
    }
    private val jumpPaint = Paint().apply {
        color = Color.argb(60, 255, 255, 255)
        style = Paint.Style.FILL
    }
    private val jumpArrowPaint = Paint().apply {
        color = Color.argb(180, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = 3f * density
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val jumpArrowPath = Path()

    private val arcInactivePaint = Paint().apply {
        color = Color.argb(30, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = 4f * density
        strokeCap = Paint.Cap.BUTT
    }
    private val arcActivePaint = Paint().apply {
        color = Color.argb(200, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = 5f * density
        strokeCap = Paint.Cap.BUTT
    }
    private val arcGap = joystickRadius + 6f * density  // gap between circle edge and arcs
    private val arcRadius = joystickRadius + 14f * density  // arc center radius
    private val arcRect = RectF()

    private val jumpRadius = 35f * density
    private var jumpCenterX = 0f
    private var jumpCenterY = 0f

    private val joystickMargin = JOYSTICK_MARGIN_DP * density
    private val joystickHitboxPadding = JOYSTICK_HITBOX_PADDING_DP * density

    init {
        isClickable = true
        isFocusable = false
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        jumpCenterX = w - 80f * density
        jumpCenterY = h - 80f * density
        joystickCenterX = joystickMargin + joystickRadius
        joystickCenterY = h - joystickMargin - joystickRadius
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val idx = event.actionIndex
                val pid = event.getPointerId(idx)
                val x = event.getX(idx)
                val y = event.getY(idx)

                val djump = hypot(x - jumpCenterX, y - jumpCenterY)
                if (djump <= jumpRadius * 1.5f && jumpPointerId == -1) {
                    jumpPointerId = pid
                    sendKey(SCAN_SPACE, 0, true)
                    invalidate()
                    return true
                }

                val hitSize = joystickRadius + joystickHitboxPadding
                val inJoystick = x >= joystickCenterX - hitSize && x <= joystickCenterX + hitSize &&
                        y >= joystickCenterY - hitSize && y <= joystickCenterY + hitSize
                if (inJoystick && joystickPointerId == -1) {
                    joystickPointerId = pid
                    joystickKnobX = joystickCenterX
                    joystickKnobY = joystickCenterY
                    joystickActive = true
                    invalidate()
                } else if (!inJoystick && cameraPointerId == -1 && jumpPointerId != pid) {
                    cameraPointerId = pid
                    cameraStartX = x
                    cameraStartY = y
                    cameraLastX = x
                    cameraLastY = y
                    cameraStartTime = System.currentTimeMillis()
                    cameraDragging = false
                    cameraMoved = false
                }
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until event.pointerCount) {
                    val pid = event.getPointerId(i)
                    val x = event.getX(i)
                    val y = event.getY(i)

                    if (pid == joystickPointerId) {
                        handleJoystickMove(x, y)
                    } else if (pid == cameraPointerId) {
                        handleCameraMove(x, y)
                    }
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                val idx = event.actionIndex
                val pid = event.getPointerId(idx)

                if (pid == joystickPointerId) {
                    releaseJoystick()
                } else if (pid == cameraPointerId) {
                    releaseCamera()
                } else if (pid == jumpPointerId) {
                    jumpPointerId = -1
                    sendKey(SCAN_SPACE, 0, false)
                    invalidate()
                }
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                if (joystickPointerId != -1) releaseJoystick()
                if (cameraPointerId != -1) releaseCamera()
                if (jumpPointerId != -1) {
                    jumpPointerId = -1
                    sendKey(SCAN_SPACE, 0, false)
                    invalidate()
                }
                return true
            }
        }
        return true
    }

    private fun handleJoystickMove(x: Float, y: Float) {
        val dx = x - joystickCenterX
        val dy = y - joystickCenterY
        val dist = hypot(dx, dy)
        val maxDist = joystickRadius

        val clampedDist = min(dist, maxDist)
        val angle = atan2(dy, dx)
        joystickKnobX = joystickCenterX + clampedDist * Math.cos(angle.toDouble()).toFloat()
        joystickKnobY = joystickCenterY + clampedDist * Math.sin(angle.toDouble()).toFloat()

        val normX = (dx / maxDist).coerceIn(-1f, 1f)
        val normY = (dy / maxDist).coerceIn(-1f, 1f)
        val magnitude = hypot(normX, normY)

        val threshold = 0.4f
        val wantW = magnitude >= JOYSTICK_DEAD_ZONE && normY < -threshold
        val wantS = magnitude >= JOYSTICK_DEAD_ZONE && normY > threshold
        val wantA = magnitude >= JOYSTICK_DEAD_ZONE && normX < -threshold
        val wantD = magnitude >= JOYSTICK_DEAD_ZONE && normX > threshold

        updateKey(SCAN_W, wantW, ::keyWDown) { keyWDown = it }
        updateKey(SCAN_A, wantA, ::keyADown) { keyADown = it }
        updateKey(SCAN_S, wantS, ::keySDown) { keySDown = it }
        updateKey(SCAN_D, wantD, ::keyDDown) { keyDDown = it }

        invalidate()
    }

    private inline fun updateKey(scanCode: Int, want: Boolean, current: () -> Boolean, setCurrent: (Boolean) -> Unit) {
        if (want && !current()) {
            sendKey(scanCode, 0, true)
            setCurrent(true)
        } else if (!want && current()) {
            sendKey(scanCode, 0, false)
            setCurrent(false)
        }
    }

    private fun handleCameraMove(x: Float, y: Float) {
        val totalDist = hypot(x - cameraStartX, y - cameraStartY)

        if (totalDist > TAP_MAX_PX) cameraMoved = true

        if (!cameraDragging && cameraMoved) {
            cameraDragging = true
            val cx = renderWidth / 2
            val cy = renderHeight / 2
            sendInput(INPUT_MOTION, 0, cx, cy)
            sendInput(INPUT_BUTTON_DOWN, MOUSE_RIGHT, cx, cy)
            cameraDeltaX = cx.toFloat()
            cameraDeltaY = cy.toFloat()
        }

        if (cameraDragging) {
            val cx = renderWidth / 2
            val cy = renderHeight / 2
            val dx = (x - cameraLastX) / width * renderWidth * 3f
            val dy = (y - cameraLastY) / height * renderHeight * 3f
            sendInput(INPUT_MOTION, 0, (cx + dx).toInt(), (cy + dy).toInt())
            sendInput(INPUT_MOTION, 0, cx, cy)
            cameraDeltaX = cx.toFloat()
            cameraDeltaY = cy.toFloat()
        }

        cameraLastX = x
        cameraLastY = y
    }

    private fun releaseJoystick() {
        joystickPointerId = -1
        joystickActive = false
        if (keyWDown) { sendKey(SCAN_W, 0, false); keyWDown = false }
        if (keyADown) { sendKey(SCAN_A, 0, false); keyADown = false }
        if (keySDown) { sendKey(SCAN_S, 0, false); keySDown = false }
        if (keyDDown) { sendKey(SCAN_D, 0, false); keyDDown = false }
        invalidate()
    }

    private fun releaseCamera() {
        val elapsed = System.currentTimeMillis() - cameraStartTime

        if (cameraDragging) {
            sendInput(INPUT_BUTTON_UP, MOUSE_RIGHT, cameraDeltaX.toInt(), cameraDeltaY.toInt())
        } else if (!cameraMoved && elapsed < TAP_MAX_MS) {
            val mx = (cameraLastX / width * renderWidth).toInt()
            val my = (cameraLastY / height * renderHeight).toInt()
            sendInput(INPUT_MOTION, 0, mx, my)
            sendInput(INPUT_BUTTON_DOWN, MOUSE_LEFT, mx, my)
            sendInput(INPUT_BUTTON_UP, MOUSE_LEFT, mx, my)
        }

        cameraPointerId = -1
        cameraDragging = false
        cameraMoved = false
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawCircle(joystickCenterX, joystickCenterY, joystickRadius, joystickBasePaint)
        canvas.drawCircle(joystickCenterX, joystickCenterY, joystickRadius, joystickOutlinePaint)
        arcRect.set(
            joystickCenterX - arcRadius, joystickCenterY - arcRadius,
            joystickCenterX + arcRadius, joystickCenterY + arcRadius
        )
        val arcSpan = 35f
        val directions = floatArrayOf(0f, 45f, 90f, 135f, 180f, 225f, 270f, 315f)
        val active = booleanArrayOf(
            keyDDown && !keyWDown && !keySDown,
            keyDDown && keySDown,
            keySDown && !keyADown && !keyDDown,
            keyADown && keySDown,
            keyADown && !keyWDown && !keySDown,
            keyADown && keyWDown,
            keyWDown && !keyADown && !keyDDown,
            keyDDown && keyWDown
        )
        for (i in directions.indices) {
            val startAngle = directions[i] - arcSpan / 2f
            val paint = if (active[i]) arcActivePaint else arcInactivePaint
            canvas.drawArc(arcRect, startAngle, arcSpan, false, paint)
        }

        val knobX = if (joystickActive) joystickKnobX else joystickCenterX
        val knobY = if (joystickActive) joystickKnobY else joystickCenterY
        canvas.drawCircle(knobX, knobY, joystickKnobRadius, joystickKnobPaint)

        val jumpAlpha = if (jumpPointerId != -1) 120 else 60
        jumpPaint.color = Color.argb(jumpAlpha, 255, 255, 255)
        canvas.drawCircle(jumpCenterX, jumpCenterY, jumpRadius, jumpPaint)
        canvas.drawCircle(jumpCenterX, jumpCenterY, jumpRadius, joystickOutlinePaint)
        val arrowSize = jumpRadius * 0.55f
        jumpArrowPath.reset()
        jumpArrowPath.moveTo(jumpCenterX - arrowSize, jumpCenterY)
        jumpArrowPath.lineTo(jumpCenterX, jumpCenterY - arrowSize)
        jumpArrowPath.lineTo(jumpCenterX + arrowSize, jumpCenterY)
        jumpArrowPath.moveTo(jumpCenterX, jumpCenterY - arrowSize)
        jumpArrowPath.lineTo(jumpCenterX, jumpCenterY + arrowSize)
        canvas.drawPath(jumpArrowPath, jumpArrowPaint)
    }
}
