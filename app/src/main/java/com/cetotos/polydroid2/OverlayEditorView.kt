package com.cetotos.polydroid2

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import kotlin.math.max

class OverlayEditorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    sealed class Which {
        object JOYSTICK : Which()
        object JUMP : Which()
        object IME : Which()
        object ITEMBAR : Which()
        object SPRINT : Which()
        data class CUSTOM(val id: String) : Which()
    }

    private class ButtonState(
        var xFrac: Float,
        var yFrac: Float,
        var scale: Float,
        val radiusDp: Float,
        val which: Which,
        var label: String = "",
        var scanCode: Int = 0,
        var toggle: Boolean = false,
        var color: Int = SettingsActivity.DEFAULT_KEY_COLOR,
        var shape: String = SettingsActivity.SHAPE_CIRCLE,
        var opacity: Float = 1f,
        var textScale: Float = 1f,
    )

    private val density = context.resources.displayMetrics.density

    var v2 = false
        private set
    private var fixedButtons = listOf<ButtonState>()
    private val customList = mutableListOf<ButtonState>()

    var selected: Which? = null
        private set

    var onSelectionChanged: ((Which?, Float) -> Unit)? = null
    var onChanged: (() -> Unit)? = null
    var onDragStateChanged: ((Boolean) -> Unit)? = null

    init {
        reload()
    }

    fun setMode(v2Mode: Boolean) {
        if (v2 == v2Mode) return
        v2 = v2Mode
        reload()
        onSelectionChanged?.invoke(selected, selectedScale())
        invalidate()
    }

    private fun reload() {
        val im = SettingsActivity.getOverlayIme(context)
        fixedButtons = if (v2) {
            listOf(ButtonState(im.xFrac, im.yFrac, im.scale, 32f, Which.IME))
        } else {
            val j = SettingsActivity.getOverlayJoystick(context)
            val jp = SettingsActivity.getOverlayJump(context)
            val ib = SettingsActivity.getOverlayItemBar(context)
            val sp = SettingsActivity.getOverlaySprint(context)
            listOf(
                ButtonState(j.xFrac, j.yFrac, j.scale, TouchControlOverlay.JOYSTICK_RADIUS_DP, Which.JOYSTICK),
                ButtonState(jp.xFrac, jp.yFrac, jp.scale, 35f, Which.JUMP),
                ButtonState(im.xFrac, im.yFrac, im.scale, 32f, Which.IME),
                ButtonState(ib.xFrac, ib.yFrac, ib.scale, TouchControlOverlay.ITEMBAR_CELL_DP, Which.ITEMBAR),
                ButtonState(sp.xFrac, sp.yFrac, sp.scale, TouchControlOverlay.SPRINT_RADIUS_DP, Which.SPRINT,
                    label = "Sprint", toggle = SettingsActivity.getSprintToggle(context)),
            )
        }
        customList.clear()
        for (ck in SettingsActivity.getCustomKeys(context, v2)) {
            customList.add(buttonOf(ck))
        }
        selected = if (v2) Which.IME else Which.JOYSTICK
        if (width > 0) for (b in buttons) clampToPreview(b)
    }

    private fun buttonOf(ck: SettingsActivity.CustomKey) = ButtonState(
        ck.xFrac, ck.yFrac, ck.scale,
        TouchControlOverlay.CUSTOM_RADIUS_DP,
        Which.CUSTOM(ck.id),
        label = ck.label,
        scanCode = ck.scanCode,
        toggle = ck.toggle,
        color = ck.color,
        shape = ck.shape,
        opacity = ck.opacity,
        textScale = ck.textScale,
    )

    private val buttons get() = fixedButtons + customList

    private val previewRect = RectF()

    private val bgPaint = Paint().apply {
        color = Color.argb(255, 25, 25, 28)
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val gridPaint = Paint().apply {
        color = Color.argb(14, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = 1f * density
    }
    private val fillPaint = Paint().apply {
        color = Color.argb(70, 255, 255, 255)
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val strokePaint = Paint().apply {
        color = Color.argb(160, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
        isAntiAlias = true
    }
    private val knobPaint = Paint().apply {
        color = Color.argb(180, 255, 255, 255)
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val selectPaint = Paint().apply {
        color = Color.argb(255, 100, 180, 255)
        style = Paint.Style.STROKE
        strokeWidth = 3f * density
        isAntiAlias = true
    }
    private val arrowPaint = Paint().apply {
        color = Color.argb(220, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = 3f * density
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        isAntiAlias = true
    }
    private val imeBgPaint = Paint().apply {
        color = Color.argb(180, 0, 0, 0)
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val labelPaint = Paint().apply {
        color = Color.argb(220, 255, 255, 255)
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    private val customFillPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val customStrokePaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
        isAntiAlias = true
    }
    private val emptyHintPaint = Paint().apply {
        color = Color.argb(120, 255, 255, 255)
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    private val imeIcon: Drawable? = ContextCompat.getDrawable(context, R.drawable.keyboard_alt_24)?.mutate()?.apply {
        colorFilter = PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
    }
    private val guidePaint = Paint().apply {
        color = Color.argb(200, 100, 180, 255)
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * density
    }
    private val arrowPath = Path()
    private val imeRect = RectF()
    private val shapeRect = RectF()

    private var dragging = false
    private var dragOffsetX = 0f
    private var dragOffsetY = 0f
    private var snapGuideX: Float? = null
    private var snapGuideY: Float? = null

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        previewRect.set(0f, 0f, w.toFloat(), h.toFloat())
        for (b in buttons) clampToPreview(b)
    }

    private fun buttonCenter(b: ButtonState): Pair<Float, Float> {
        val x = previewRect.left + b.xFrac * previewRect.width()
        val y = previewRect.top + b.yFrac * previewRect.height()
        return x to y
    }

    private fun buttonRadius(b: ButtonState): Float = b.radiusDp * density * b.scale

    private fun halfExtentsPx(b: ButtonState): Pair<Float, Float> = when (b.which) {
        Which.IME -> {
            val h = 64f * density * b.scale / 2f
            h to h
        }
        Which.ITEMBAR -> {
            val cell = TouchControlOverlay.ITEMBAR_CELL_DP * density * b.scale
            (cell * TouchControlOverlay.ITEMBAR_CELLS / 2f) to (cell / 2f)
        }
        else -> {
            val r = buttonRadius(b)
            r to r
        }
    }

    private fun clampToPreview(b: ButtonState) {
        if (previewRect.width() <= 0f || previewRect.height() <= 0f) return
        val (hx, hy) = halfExtentsPx(b)
        val xMin = hx / previewRect.width()
        val yMin = hy / previewRect.height()
        val xMax = 1f - xMin
        val yMax = 1f - yMin
        b.xFrac = if (xMin <= xMax) b.xFrac.coerceIn(xMin, xMax) else 0.5f
        b.yFrac = if (yMin <= yMax) b.yFrac.coerceIn(yMin, yMax) else 0.5f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(previewRect, bgPaint)
        val cols = 8
        val rows = 4
        for (c in 1 until cols) {
            val x = previewRect.width() * c / cols
            canvas.drawLine(x, previewRect.top, x, previewRect.bottom, gridPaint)
        }
        for (r in 1 until rows) {
            val y = previewRect.height() * r / rows
            canvas.drawLine(previewRect.left, y, previewRect.right, y, gridPaint)
        }

        for (b in fixedButtons) {
            when (b.which) {
                Which.JOYSTICK -> drawJoystick(canvas, b)
                Which.JUMP -> drawJump(canvas, b)
                Which.IME -> drawIme(canvas, b)
                Which.ITEMBAR -> drawItemBar(canvas, b)
                else -> drawKeyButton(canvas, b)
            }
        }
        for (b in customList) drawKeyButton(canvas, b)

        snapGuideX?.let { canvas.drawLine(it, previewRect.top, it, previewRect.bottom, guidePaint) }
        snapGuideY?.let { canvas.drawLine(previewRect.left, it, previewRect.right, it, guidePaint) }

        if (v2 && customList.isEmpty()) {
            emptyHintPaint.textSize = 16f * density
            canvas.drawText(
                "Tap \"Add key\" to add your own buttons",
                previewRect.centerX(),
                previewRect.height() * 0.35f,
                emptyHintPaint,
            )
        }
    }

    private fun drawItemBar(canvas: Canvas, b: ButtonState) {
        val (cx, cy) = buttonCenter(b)
        val (hx, hy) = halfExtentsPx(b)
        val cell = hx * 2f / TouchControlOverlay.ITEMBAR_CELLS
        val rect = RectF(cx - hx, cy - hy, cx + hx, cy + hy)
        val radius = cell * 0.25f
        canvas.drawRoundRect(rect, radius, radius, fillPaint)
        labelPaint.textSize = cell * 0.45f
        for (c in 0 until TouchControlOverlay.ITEMBAR_CELLS) {
            val left = rect.left + c * cell
            if (c > 0) canvas.drawLine(left, rect.top, left, rect.bottom, strokePaint)
            canvas.drawText(
                (c + 1).toString(),
                left + cell / 2f,
                cy + labelPaint.textSize / 3f,
                labelPaint,
            )
        }
        canvas.drawRoundRect(rect, radius, radius, strokePaint)
        if (selected == b.which) canvas.drawRoundRect(rect, radius, radius, selectPaint)
    }

    private fun drawJoystick(canvas: Canvas, b: ButtonState) {
        val (cx, cy) = buttonCenter(b)
        val r = buttonRadius(b)
        canvas.drawCircle(cx, cy, r, fillPaint)
        canvas.drawCircle(cx, cy, r, strokePaint)
        canvas.drawCircle(cx, cy, r * (TouchControlOverlay.JOYSTICK_KNOB_RADIUS_DP / TouchControlOverlay.JOYSTICK_RADIUS_DP), knobPaint)
        if (selected == b.which) canvas.drawCircle(cx, cy, r + 4f * density, selectPaint)
    }

    private fun drawJump(canvas: Canvas, b: ButtonState) {
        val (cx, cy) = buttonCenter(b)
        val r = buttonRadius(b)
        canvas.drawCircle(cx, cy, r, fillPaint)
        canvas.drawCircle(cx, cy, r, strokePaint)
        val a = r * 0.55f
        arrowPath.reset()
        arrowPath.moveTo(cx - a, cy)
        arrowPath.lineTo(cx, cy - a)
        arrowPath.lineTo(cx + a, cy)
        arrowPath.moveTo(cx, cy - a)
        arrowPath.lineTo(cx, cy + a)
        canvas.drawPath(arrowPath, arrowPaint)
        if (selected == b.which) canvas.drawCircle(cx, cy, r + 4f * density, selectPaint)
    }

    private fun drawIme(canvas: Canvas, b: ButtonState) {
        val (cx, cy) = buttonCenter(b)
        val half = 64f * density * b.scale / 2f
        imeRect.set(cx - half, cy - half, cx + half, cy + half)
        canvas.drawRect(imeRect, imeBgPaint)
        val icon = imeIcon
        if (icon != null) {
            val inset = half * 0.35f
            icon.setBounds(
                (cx - half + inset).toInt(),
                (cy - half + inset).toInt(),
                (cx + half - inset).toInt(),
                (cy + half - inset).toInt(),
            )
            icon.draw(canvas)
        }
        if (selected == b.which) canvas.drawRect(imeRect, selectPaint)
    }

    private fun drawKeyButton(canvas: Canvas, b: ButtonState) {
        val (cx, cy) = buttonCenter(b)
        val r = buttonRadius(b)
        val cr = Color.red(b.color); val cg = Color.green(b.color); val cb = Color.blue(b.color)
        customFillPaint.color = Color.argb((70 * b.opacity).toInt(), cr, cg, cb)
        customStrokePaint.color = Color.argb((160 * b.opacity).toInt().coerceAtLeast(40), cr, cg, cb)
        if (b.shape == SettingsActivity.SHAPE_SQUARE) {
            shapeRect.set(cx - r, cy - r, cx + r, cy + r)
            val corner = r * 0.25f
            canvas.drawRoundRect(shapeRect, corner, corner, customFillPaint)
            canvas.drawRoundRect(shapeRect, corner, corner, customStrokePaint)
            if (selected == b.which) {
                shapeRect.inset(-4f * density, -4f * density)
                canvas.drawRoundRect(shapeRect, corner, corner, selectPaint)
            }
        } else {
            canvas.drawCircle(cx, cy, r, customFillPaint)
            canvas.drawCircle(cx, cy, r, customStrokePaint)
            if (selected == b.which) canvas.drawCircle(cx, cy, r + 4f * density, selectPaint)
        }
        labelPaint.color = Color.argb((220 * b.opacity).toInt().coerceAtLeast(60), 255, 255, 255)
        labelPaint.textSize = r * 0.7f
        if (b.label.isNotEmpty()) {
            val w = labelPaint.measureText(b.label)
            val maxW = r * 1.8f
            if (w > maxW) labelPaint.textSize *= maxW / w
            labelPaint.textSize *= b.textScale
            canvas.drawText(b.label, cx, cy + labelPaint.textSize / 3f, labelPaint)
        }
        labelPaint.color = Color.argb(220, 255, 255, 255)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                val hit = findHit(x, y)
                if (hit != null) {
                    selected = hit.which
                    val (cx, cy) = buttonCenter(hit)
                    dragOffsetX = x - cx
                    dragOffsetY = y - cy
                    dragging = true
                    onDragStateChanged?.invoke(true)
                    onSelectionChanged?.invoke(hit.which, hit.scale)
                } else {
                    dragging = false
                    if (selected != null) {
                        selected = null
                        onSelectionChanged?.invoke(null, 1f)
                    }
                }
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (dragging) {
                    val b = current() ?: return true
                    val rawCx = x - dragOffsetX
                    val rawCy = y - dragOffsetY
                    var cx = rawCx
                    var cy = rawCy
                    val snapPx = 10f * density
                    var bestDx = snapPx
                    var bestDy = snapPx
                    snapGuideX = null
                    snapGuideY = null
                    for (o in buttons) {
                        if (o === b) continue
                        val (ocx, ocy) = buttonCenter(o)
                        val ddx = kotlin.math.abs(rawCx - ocx)
                        if (ddx < bestDx) {
                            bestDx = ddx
                            cx = ocx
                            snapGuideX = ocx
                        }
                        val ddy = kotlin.math.abs(rawCy - ocy)
                        if (ddy < bestDy) {
                            bestDy = ddy
                            cy = ocy
                            snapGuideY = ocy
                        }
                    }
                    b.xFrac = (cx - previewRect.left) / previewRect.width()
                    b.yFrac = (cy - previewRect.top) / previewRect.height()
                    clampToPreview(b)
                    save(b)
                    onChanged?.invoke()
                    invalidate()
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (dragging) {
                    dragging = false
                    snapGuideX = null
                    snapGuideY = null
                    onDragStateChanged?.invoke(false)
                    invalidate()
                }
                return true
            }
        }
        return false
    }

    fun hasButtonAt(x: Float, y: Float): Boolean = findHit(x, y) != null

    private fun findHit(x: Float, y: Float): ButtonState? {
        val order = buttons.sortedByDescending { it.which == selected }
        val minHalf = 20f * density
        for (b in order) {
            val (cx, cy) = buttonCenter(b)
            val (hx, hy) = halfExtentsPx(b)
            val hitX = max(hx, minHalf)
            val hitY = max(hy, minHalf)
            if (kotlin.math.abs(x - cx) <= hitX && kotlin.math.abs(y - cy) <= hitY) return b
        }
        return null
    }

    private fun current(): ButtonState? = buttons.firstOrNull { it.which == selected }

    fun setSelectedScale(scale: Float) {
        val b = current() ?: return
        b.scale = scale.coerceIn(0.5f, 2f)
        clampToPreview(b)
        save(b)
        onChanged?.invoke()
        invalidate()
    }

    fun selectedScale(): Float = current()?.scale ?: 1f

    fun addCustomKey(ck: SettingsActivity.CustomKey) {
        customList.add(buttonOf(ck))
        val added = customList.last()
        clampToPreview(added)
        saveCustomAll()
        selected = added.which
        onSelectionChanged?.invoke(added.which, added.scale)
        onChanged?.invoke()
        invalidate()
    }

    fun cloneSelected() {
        val b = current() ?: return
        if (b.which !is Which.CUSTOM) return
        val (cx, cy) = buttonCenter(b)
        val (hx, hy) = halfExtentsPx(b)
        val gap = 8f * density
        val candidates = listOf(
            (cx + 2 * hx + gap) to cy,
            (cx - 2 * hx - gap) to cy,
            cx to (cy + 2 * hy + gap),
            cx to (cy - 2 * hy - gap),
        )
        for ((ncx, ncy) in candidates) {
            if (ncx - hx < previewRect.left || ncx + hx > previewRect.right ||
                ncy - hy < previewRect.top || ncy + hy > previewRect.bottom) continue
            var overlaps = false
            for (o in buttons) {
                val (ocx, ocy) = buttonCenter(o)
                val (ohx, ohy) = halfExtentsPx(o)
                if (kotlin.math.abs(ncx - ocx) < hx + ohx && kotlin.math.abs(ncy - ocy) < hy + ohy) {
                    overlaps = true
                    break
                }
            }
            if (overlaps) continue
            addCustomKey(customKeyOf(b).copy(
                id = "ck_${System.currentTimeMillis()}",
                xFrac = (ncx - previewRect.left) / previewRect.width(),
                yFrac = (ncy - previewRect.top) / previewRect.height(),
            ))
            return
        }
    }

    fun removeCustomKey(id: String) {
        val idx = customList.indexOfFirst { (it.which as? Which.CUSTOM)?.id == id }
        if (idx < 0) return
        customList.removeAt(idx)
        if ((selected as? Which.CUSTOM)?.id == id) {
            selected = if (v2) customList.firstOrNull()?.which else Which.JOYSTICK
            onSelectionChanged?.invoke(selected, selectedScale())
        }
        saveCustomAll()
        onChanged?.invoke()
        invalidate()
    }

    private fun updateSelectedCustom(block: (ButtonState) -> Unit) {
        val b = current() ?: return
        if (b.which !is Which.CUSTOM) return
        block(b)
        saveCustomAll()
        onChanged?.invoke()
        invalidate()
    }

    fun setSelectedLabel(label: String) = updateSelectedCustom { it.label = label }
    fun setSelectedScan(scanCode: Int) = updateSelectedCustom { it.scanCode = scanCode }
    fun setSelectedColor(color: Int) = updateSelectedCustom { it.color = color }
    fun setSelectedShape(shape: String) = updateSelectedCustom { it.shape = shape }
    fun setSelectedTextScale(textScale: Float) = updateSelectedCustom { it.textScale = textScale.coerceIn(0.5f, 2f) }

    fun setSelectedToggle(toggle: Boolean) {
        val b = current() ?: return
        when (b.which) {
            Which.SPRINT -> {
                b.toggle = toggle
                SettingsActivity.setSprintToggle(context, toggle)
            }
            is Which.CUSTOM -> {
                b.toggle = toggle
                saveCustomAll()
            }
            else -> return
        }
        onChanged?.invoke()
    }

    fun selectedToggle(): Boolean? {
        val b = current() ?: return null
        return when (b.which) {
            Which.SPRINT, is Which.CUSTOM -> b.toggle
            else -> null
        }
    }

    fun selectedCustomId(): String? = (selected as? Which.CUSTOM)?.id

    fun selectedCustom(): SettingsActivity.CustomKey? {
        val b = current() ?: return null
        if (b.which !is Which.CUSTOM) return null
        return customKeyOf(b)
    }

    private fun customKeyOf(b: ButtonState) = SettingsActivity.CustomKey(
        id = (b.which as Which.CUSTOM).id,
        label = b.label,
        scanCode = b.scanCode,
        xFrac = b.xFrac,
        yFrac = b.yFrac,
        scale = b.scale,
        toggle = b.toggle,
        color = b.color,
        shape = b.shape,
        opacity = b.opacity,
        textScale = b.textScale,
    )

    fun customKeys(): List<SettingsActivity.CustomKey> = customList.map { customKeyOf(it) }

    fun resetAll() {
        for (b in fixedButtons) {
            when (b.which) {
                Which.JOYSTICK -> {
                    b.xFrac = SettingsActivity.DEFAULT_JOYSTICK_X
                    b.yFrac = SettingsActivity.DEFAULT_JOYSTICK_Y
                }
                Which.JUMP -> {
                    b.xFrac = SettingsActivity.DEFAULT_JUMP_X
                    b.yFrac = SettingsActivity.DEFAULT_JUMP_Y
                }
                Which.IME -> {
                    b.xFrac = SettingsActivity.DEFAULT_IME_X
                    b.yFrac = SettingsActivity.DEFAULT_IME_Y
                }
                Which.ITEMBAR -> {
                    b.xFrac = SettingsActivity.DEFAULT_ITEMBAR_X
                    b.yFrac = SettingsActivity.DEFAULT_ITEMBAR_Y
                }
                Which.SPRINT -> {
                    b.xFrac = SettingsActivity.DEFAULT_SPRINT_X
                    b.yFrac = SettingsActivity.DEFAULT_SPRINT_Y
                }
                else -> {}
            }
            b.scale = 1f
            clampToPreview(b)
            save(b)
        }
        customList.clear()
        if (selected is Which.CUSTOM || selected == null) {
            selected = if (v2) Which.IME else Which.JOYSTICK
        }
        saveCustomAll()
        onSelectionChanged?.invoke(selected, selectedScale())
        onChanged?.invoke()
        invalidate()
    }

    private fun save(b: ButtonState) {
        val prefs = context.getSharedPreferences(SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE)
        val e = prefs.edit()
        when (b.which) {
            Which.JOYSTICK -> {
                e.putFloat(SettingsActivity.KEY_JOYSTICK_X, b.xFrac)
                    .putFloat(SettingsActivity.KEY_JOYSTICK_Y, b.yFrac)
                    .putFloat(SettingsActivity.KEY_JOYSTICK_SCALE, b.scale)
            }
            Which.JUMP -> {
                e.putFloat(SettingsActivity.KEY_JUMP_X, b.xFrac)
                    .putFloat(SettingsActivity.KEY_JUMP_Y, b.yFrac)
                    .putFloat(SettingsActivity.KEY_JUMP_SCALE, b.scale)
            }
            Which.IME -> {
                e.putFloat(SettingsActivity.KEY_IME_X, b.xFrac)
                    .putFloat(SettingsActivity.KEY_IME_Y, b.yFrac)
                    .putFloat(SettingsActivity.KEY_IME_SCALE, b.scale)
            }
            Which.ITEMBAR -> {
                e.putFloat(SettingsActivity.KEY_ITEMBAR_X, b.xFrac)
                    .putFloat(SettingsActivity.KEY_ITEMBAR_Y, b.yFrac)
                    .putFloat(SettingsActivity.KEY_ITEMBAR_SCALE, b.scale)
            }
            Which.SPRINT -> {
                e.putFloat(SettingsActivity.KEY_SPRINT_X, b.xFrac)
                    .putFloat(SettingsActivity.KEY_SPRINT_Y, b.yFrac)
                    .putFloat(SettingsActivity.KEY_SPRINT_SCALE, b.scale)
            }
            is Which.CUSTOM -> {
                saveCustomAll()
                return
            }
        }
        e.apply()
    }

    private fun saveCustomAll() {
        SettingsActivity.saveCustomKeys(context, customKeys(), v2)
    }
}
