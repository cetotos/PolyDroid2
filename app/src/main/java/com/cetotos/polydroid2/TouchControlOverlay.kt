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
    private val cameraSensitivity: Float = 3f,
) : View(context) {

    companion object {
        const val JOYSTICK_RADIUS_DP = 75f
        const val JOYSTICK_KNOB_RADIUS_DP = 33f
        private const val JOYSTICK_DEAD_ZONE = 0.15f
        private const val JOYSTICK_HITBOX_PADDING_DP = 20f
        const val JUMP_RADIUS_DP = 35f
        const val ITEMBAR_CELL_DP = 60f
        const val ITEMBAR_CELLS = 3
        const val SPRINT_RADIUS_DP = 32f
        const val CUSTOM_RADIUS_DP = 30f

        private const val TAP_MAX_MS = 250L
        private const val TAP_MAX_PX = 25f

        private const val SCAN_W = 17
        private const val SCAN_A = 30
        private const val SCAN_S = 31
        private const val SCAN_D = 32
        private const val SCAN_SPACE = 57
        private const val SCAN_SHIFT = 42
        private const val SCAN_I = 23
        private const val SCAN_O = 24
        private val ITEMBAR_SCANS = intArrayOf(2, 3, 4)
        private const val INPUT_BUTTON_DOWN = 2
        private const val INPUT_BUTTON_UP = 3
        private const val INPUT_MOTION = 1
        private const val MOUSE_LEFT = 1
        private const val MOUSE_RIGHT = 3
    }

    private class KeyButton(
        val scanCode: Int,
        val label: String,
        val toggle: Boolean,
        val radiusDp: Float,
        layout: SettingsActivity.OverlayButton,
        density: Float,
    ) {
        val scale = layout.scale
        val xFrac = layout.xFrac
        val yFrac = layout.yFrac
        val radius = radiusDp * density * layout.scale
        var cx = 0f
        var cy = 0f
        var pointerId = -1
        var toggleActive = false
    }

    private val density = context.resources.displayMetrics.density
    private val joystickLayout = SettingsActivity.getOverlayJoystick(context)
    private val jumpLayout = SettingsActivity.getOverlayJump(context)
    private val itemBarLayout = SettingsActivity.getOverlayItemBar(context)
    private val joystickRadius = JOYSTICK_RADIUS_DP * density * joystickLayout.scale
    private val joystickKnobRadius = JOYSTICK_KNOB_RADIUS_DP * density * joystickLayout.scale
    private val itemBarCell = ITEMBAR_CELL_DP * density * itemBarLayout.scale
    private val itemBarWidth = itemBarCell * ITEMBAR_CELLS
    private val itemBarHeight = itemBarCell

    private val sprintBtn = KeyButton(
        SCAN_SHIFT, "Sprint",
        SettingsActivity.getSprintToggle(context),
        SPRINT_RADIUS_DP,
        SettingsActivity.getOverlaySprint(context),
        density,
    )
    private val customBtns: List<KeyButton> = SettingsActivity.getCustomKeys(context).map { ck ->
        KeyButton(
            ck.scanCode, ck.label, ck.toggle, CUSTOM_RADIUS_DP,
            SettingsActivity.OverlayButton(ck.xFrac, ck.yFrac, ck.scale),
            density,
        )
    }
    private val keyButtons = listOf(sprintBtn) + customBtns

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

    private var pinchPointer1Id = -1
    private var pinchPointer2Id = -1
    private var pinchLastDist = 0f
    private var pinchAccum = 0f
    private val pinchStepPx = 40f * density

    private var itemBarCenterX = 0f
    private var itemBarCenterY = 0f
    private val itemBarRect = RectF()
    private val itemBarActiveCell = IntArray(ITEMBAR_CELLS) { -1 }
    private val itemBarTextPaint = Paint().apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
        textSize = 22f * density
    }
    private val itemBarCellPaint = Paint().apply {
        color = Color.argb(60, 255, 255, 255)
        style = Paint.Style.FILL
    }
    private val itemBarCellActivePaint = Paint().apply {
        color = Color.argb(180, 255, 255, 255)
        style = Paint.Style.FILL
    }
    private val itemBarDividerPaint = Paint().apply {
        color = Color.argb(80, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
    }

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
    private val keyFillPaint = Paint().apply {
        color = Color.argb(60, 255, 255, 255)
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val keyFillActivePaint = Paint().apply {
        color = Color.argb(160, 255, 255, 255)
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val keyLabelPaint = Paint().apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

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

    private val jumpRadius = JUMP_RADIUS_DP * density * jumpLayout.scale
    private var jumpCenterX = 0f
    private var jumpCenterY = 0f

    private val joystickHitboxPadding = JOYSTICK_HITBOX_PADDING_DP * density

    init {
        isClickable = true
        isFocusable = false
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        joystickCenterX = joystickLayout.xFrac * w
        joystickCenterY = joystickLayout.yFrac * h
        jumpCenterX = jumpLayout.xFrac * w
        jumpCenterY = jumpLayout.yFrac * h
        itemBarCenterX = itemBarLayout.xFrac * w
        itemBarCenterY = itemBarLayout.yFrac * h
        itemBarRect.set(
            itemBarCenterX - itemBarWidth / 2f,
            itemBarCenterY - itemBarHeight / 2f,
            itemBarCenterX + itemBarWidth / 2f,
            itemBarCenterY + itemBarHeight / 2f,
        )
        for (b in keyButtons) {
            b.cx = b.xFrac * w
            b.cy = b.yFrac * h
        }
    }

    private fun itemBarCellIndex(x: Float, y: Float): Int {
        if (!itemBarRect.contains(x, y)) return -1
        val rel = (x - itemBarRect.left) / itemBarCell
        return rel.toInt().coerceIn(0, ITEMBAR_CELLS - 1)
    }

    private fun hitKeyButton(x: Float, y: Float): KeyButton? {
        for (b in keyButtons) {
            if (hypot(x - b.cx, y - b.cy) <= b.radius * 1.2f) return b
        }
        return null
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

                val cellIdx = itemBarCellIndex(x, y)
                if (cellIdx >= 0) {
                    if (itemBarActiveCell[cellIdx] == -1) {
                        itemBarActiveCell[cellIdx] = pid
                        sendKey(ITEMBAR_SCANS[cellIdx], 0, true)
                        invalidate()
                    }
                    return true
                }

                val kb = hitKeyButton(x, y)
                if (kb != null && kb.pointerId == -1) {
                    if (kb.toggle) {
                        kb.toggleActive = !kb.toggleActive
                        sendKey(kb.scanCode, 0, kb.toggleActive)
                        kb.pointerId = pid
                    } else {
                        kb.pointerId = pid
                        sendKey(kb.scanCode, 0, true)
                    }
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
                } else if (!inJoystick && jumpPointerId != pid) {
                    if (cameraPointerId == -1 && pinchPointer1Id == -1) {
                        cameraPointerId = pid
                        cameraStartX = x
                        cameraStartY = y
                        cameraLastX = x
                        cameraLastY = y
                        cameraStartTime = System.currentTimeMillis()
                        cameraDragging = false
                        cameraMoved = false
                    } else if (pinchPointer1Id == -1 && cameraPointerId != -1 && cameraPointerId != pid) {
                        val otherIdx = event.findPointerIndex(cameraPointerId)
                        if (otherIdx >= 0) {
                            endCameraDragIfActive()
                            pinchPointer1Id = cameraPointerId
                            pinchPointer2Id = pid
                            cameraPointerId = -1
                            val ox = event.getX(otherIdx)
                            val oy = event.getY(otherIdx)
                            pinchLastDist = hypot(x - ox, y - oy)
                            pinchAccum = 0f
                        }
                    }
                }
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (pinchPointer1Id != -1 && pinchPointer2Id != -1) {
                    val i1 = event.findPointerIndex(pinchPointer1Id)
                    val i2 = event.findPointerIndex(pinchPointer2Id)
                    if (i1 >= 0 && i2 >= 0) {
                        val dist = hypot(event.getX(i1) - event.getX(i2), event.getY(i1) - event.getY(i2))
                        pinchAccum += dist - pinchLastDist
                        pinchLastDist = dist
                        if (pinchAccum >= pinchStepPx) {
                            sendZoomTap(SCAN_I)
                            pinchAccum = 0f
                        } else if (pinchAccum <= -pinchStepPx) {
                            sendZoomTap(SCAN_O)
                            pinchAccum = 0f
                        }
                    }
                }
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
                } else if (pid == pinchPointer1Id || pid == pinchPointer2Id) {
                    pinchPointer1Id = -1
                    pinchPointer2Id = -1
                    pinchAccum = 0f
                } else if (pid == cameraPointerId) {
                    releaseCamera()
                } else if (pid == jumpPointerId) {
                    jumpPointerId = -1
                    sendKey(SCAN_SPACE, 0, false)
                    invalidate()
                } else {
                    for (c in 0 until ITEMBAR_CELLS) {
                        if (itemBarActiveCell[c] == pid) {
                            itemBarActiveCell[c] = -1
                            sendKey(ITEMBAR_SCANS[c], 0, false)
                            invalidate()
                        }
                    }
                    for (b in keyButtons) {
                        if (b.pointerId == pid) {
                            b.pointerId = -1
                            if (!b.toggle) sendKey(b.scanCode, 0, false)
                            invalidate()
                        }
                    }
                }
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                if (joystickPointerId != -1) releaseJoystick()
                if (cameraPointerId != -1) releaseCamera()
                if (pinchPointer1Id != -1 || pinchPointer2Id != -1) {
                    pinchPointer1Id = -1
                    pinchPointer2Id = -1
                    pinchAccum = 0f
                }
                if (jumpPointerId != -1) {
                    jumpPointerId = -1
                    sendKey(SCAN_SPACE, 0, false)
                }
                for (c in 0 until ITEMBAR_CELLS) {
                    if (itemBarActiveCell[c] != -1) {
                        itemBarActiveCell[c] = -1
                        sendKey(ITEMBAR_SCANS[c], 0, false)
                    }
                }
                for (b in keyButtons) {
                    if (b.pointerId != -1) {
                        b.pointerId = -1
                        if (!b.toggle) sendKey(b.scanCode, 0, false)
                    }
                    if (b.toggle && b.toggleActive) {
                        b.toggleActive = false
                        sendKey(b.scanCode, 0, false)
                    }
                }
                invalidate()
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
            val dx = (x - cameraLastX) / width * renderWidth * cameraSensitivity
            val dy = (y - cameraLastY) / height * renderHeight * cameraSensitivity
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

    private fun endCameraDragIfActive() {
        if (cameraDragging) {
            sendInput(INPUT_BUTTON_UP, MOUSE_RIGHT, cameraDeltaX.toInt(), cameraDeltaY.toInt())
        }
        cameraDragging = false
        cameraMoved = false
    }

    private fun sendZoomTap(scan: Int) {
        sendKey(scan, 0, true)
        postDelayed(Runnable { sendKey(scan, 0, false) }, 50L)
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

        val radius = itemBarCell * 0.25f
        canvas.drawRoundRect(itemBarRect, radius, radius, itemBarCellPaint)
        for (c in 0 until ITEMBAR_CELLS) {
            val left = itemBarRect.left + c * itemBarCell
            if (itemBarActiveCell[c] != -1) {
                val cellRect = RectF(left, itemBarRect.top, left + itemBarCell, itemBarRect.bottom)
                canvas.drawRoundRect(cellRect, radius, radius, itemBarCellActivePaint)
            }
            if (c > 0) canvas.drawLine(left, itemBarRect.top, left, itemBarRect.bottom, itemBarDividerPaint)
            itemBarTextPaint.textSize = itemBarCell * 0.45f
            canvas.drawText(
                (c + 1).toString(),
                left + itemBarCell / 2f,
                itemBarRect.centerY() + itemBarTextPaint.textSize / 3f,
                itemBarTextPaint
            )
        }
        canvas.drawRoundRect(itemBarRect, radius, radius, itemBarDividerPaint)

        for (b in keyButtons) {
            val isActive = if (b.toggle) b.toggleActive else b.pointerId != -1
            canvas.drawCircle(b.cx, b.cy, b.radius, if (isActive) keyFillActivePaint else keyFillPaint)
            canvas.drawCircle(b.cx, b.cy, b.radius, joystickOutlinePaint)
            keyLabelPaint.textSize = b.radius * 0.7f
            canvas.drawText(b.label, b.cx, b.cy + keyLabelPaint.textSize / 3f, keyLabelPaint)
        }
    }
}
