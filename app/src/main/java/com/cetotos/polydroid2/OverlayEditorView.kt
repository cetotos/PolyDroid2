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
import kotlin.math.min

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
    )

    private val density = context.resources.displayMetrics.density

    private val joystick: ButtonState
    private val jump: ButtonState
    private val ime: ButtonState
    private val itemBar: ButtonState
    private val sprint: ButtonState
    private val customList = mutableListOf<ButtonState>()

    init {
        val j = SettingsActivity.getOverlayJoystick(context)
        val jp = SettingsActivity.getOverlayJump(context)
        val im = SettingsActivity.getOverlayIme(context)
        val ib = SettingsActivity.getOverlayItemBar(context)
        val sp = SettingsActivity.getOverlaySprint(context)
        joystick = ButtonState(j.xFrac, j.yFrac, j.scale, TouchControlOverlay.JOYSTICK_RADIUS_DP, Which.JOYSTICK)
        jump = ButtonState(jp.xFrac, jp.yFrac, jp.scale, 35f, Which.JUMP)
        ime = ButtonState(im.xFrac, im.yFrac, im.scale, 32f, Which.IME)
        itemBar = ButtonState(ib.xFrac, ib.yFrac, ib.scale, TouchControlOverlay.ITEMBAR_CELL_DP, Which.ITEMBAR)
        sprint = ButtonState(sp.xFrac, sp.yFrac, sp.scale, TouchControlOverlay.SPRINT_RADIUS_DP, Which.SPRINT, label = "Sprint",
            toggle = SettingsActivity.getSprintToggle(context))
        for (ck in SettingsActivity.getCustomKeys(context)) {
            customList.add(ButtonState(
                ck.xFrac, ck.yFrac, ck.scale,
                TouchControlOverlay.CUSTOM_RADIUS_DP,
                Which.CUSTOM(ck.id),
                label = ck.label,
                scanCode = ck.scanCode,
                toggle = ck.toggle,
            ))
        }
    }

    private val fixedButtons = listOf(joystick, jump, ime, itemBar, sprint)
    private val buttons get() = fixedButtons + customList

    var selected: Which = Which.JOYSTICK
        private set

    var onSelectionChanged: ((Which, Float) -> Unit)? = null
    var onChanged: (() -> Unit)? = null

    private val previewRect = RectF()
    private var previewScale = 1f

    private val screenPxW: Float
    private val screenPxH: Float
    private val aspectRatio: Float

    init {
        val dm = context.resources.displayMetrics
        val a = dm.widthPixels.toFloat()
        val b = dm.heightPixels.toFloat()
        screenPxW = max(a, b)
        screenPxH = min(a, b)
        aspectRatio = screenPxW / screenPxH
    }

    private val bgPaint = Paint().apply {
        color = Color.argb(255, 25, 25, 28)
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val borderPaint = Paint().apply {
        color = Color.argb(120, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
        isAntiAlias = true
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
    private val imeIcon: Drawable? = ContextCompat.getDrawable(context, R.drawable.keyboard_alt_24)?.mutate()?.apply {
        colorFilter = PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
    }
    private val arrowPath = Path()
    private val imeRect = RectF()

    private var dragging = false
    private var dragOffsetX = 0f
    private var dragOffsetY = 0f

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val desiredPreviewH = (w / aspectRatio).toInt()
        val h = desiredPreviewH + (16 * density).toInt()
        setMeasuredDimension(w, h)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        val pw = w.toFloat()
        val ph = pw / aspectRatio
        val top = ((h - ph) / 2f).coerceAtLeast(0f)
        previewRect.set(0f, top, pw, top + ph)
        previewScale = pw / screenPxW
        for (b in buttons) clampToPreview(b)
    }

    private fun buttonCenter(b: ButtonState): Pair<Float, Float> {
        val x = previewRect.left + b.xFrac * previewRect.width()
        val y = previewRect.top + b.yFrac * previewRect.height()
        return x to y
    }

    private fun buttonPreviewRadius(b: ButtonState): Float {
        val actualPx = b.radiusDp * density * b.scale
        return actualPx * previewScale
    }

    private fun halfExtentsPx(b: ButtonState): Pair<Float, Float> = when (b.which) {
        Which.IME -> {
            val h = 64f * density * b.scale * previewScale / 2f
            h to h
        }
        Which.ITEMBAR -> {
            val cell = TouchControlOverlay.ITEMBAR_CELL_DP * density * b.scale * previewScale
            (cell * TouchControlOverlay.ITEMBAR_CELLS / 2f) to (cell / 2f)
        }
        else -> {
            val r = buttonPreviewRadius(b)
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
        canvas.drawRoundRect(previewRect, 8f * density, 8f * density, bgPaint)
        canvas.drawRoundRect(previewRect, 8f * density, 8f * density, borderPaint)

        drawJoystick(canvas, joystick)
        drawJump(canvas, jump)
        drawIme(canvas, ime)
        drawItemBar(canvas, itemBar)
        for (b in listOf(sprint) + customList) drawLabelButton(canvas, b)
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
        val r = buttonPreviewRadius(b)
        canvas.drawCircle(cx, cy, r, fillPaint)
        canvas.drawCircle(cx, cy, r, strokePaint)
        canvas.drawCircle(cx, cy, r * (TouchControlOverlay.JOYSTICK_KNOB_RADIUS_DP / TouchControlOverlay.JOYSTICK_RADIUS_DP), knobPaint)
        if (selected == b.which) canvas.drawCircle(cx, cy, r + 4f * density, selectPaint)
    }

    private fun drawJump(canvas: Canvas, b: ButtonState) {
        val (cx, cy) = buttonCenter(b)
        val r = buttonPreviewRadius(b)
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
        val sizeActual = 64f * density * b.scale
        val halfPrev = sizeActual * previewScale / 2f
        imeRect.set(cx - halfPrev, cy - halfPrev, cx + halfPrev, cy + halfPrev)
        canvas.drawRect(imeRect, imeBgPaint)
        val icon = imeIcon
        if (icon != null) {
            val inset = halfPrev * 0.35f
            icon.setBounds(
                (cx - halfPrev + inset).toInt(),
                (cy - halfPrev + inset).toInt(),
                (cx + halfPrev - inset).toInt(),
                (cy + halfPrev - inset).toInt(),
            )
            icon.draw(canvas)
        }
        if (selected == b.which) canvas.drawRect(imeRect, selectPaint)
    }

    private fun drawLabelButton(canvas: Canvas, b: ButtonState) {
        val (cx, cy) = buttonCenter(b)
        val r = buttonPreviewRadius(b)
        canvas.drawCircle(cx, cy, r, fillPaint)
        canvas.drawCircle(cx, cy, r, strokePaint)
        labelPaint.textSize = r * 0.7f
        canvas.drawText(b.label, cx, cy + labelPaint.textSize / 3f, labelPaint)
        if (selected == b.which) canvas.drawCircle(cx, cy, r + 4f * density, selectPaint)
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
                    onSelectionChanged?.invoke(hit.which, hit.scale)
                    invalidate()
                } else {
                    dragging = false
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (dragging) {
                    val b = current() ?: return true
                    b.xFrac = (x - dragOffsetX - previewRect.left) / previewRect.width()
                    b.yFrac = (y - dragOffsetY - previewRect.top) / previewRect.height()
                    clampToPreview(b)
                    save(b)
                    onChanged?.invoke()
                    invalidate()
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                dragging = false
                return true
            }
        }
        return false
    }

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

    fun select(which: Which) {
        if (buttons.any { it.which == which }) {
            selected = which
            onSelectionChanged?.invoke(which, selectedScale())
            invalidate()
        }
    }

    fun addCustomKey(ck: SettingsActivity.CustomKey) {
        customList.add(ButtonState(
            ck.xFrac, ck.yFrac, ck.scale,
            TouchControlOverlay.CUSTOM_RADIUS_DP,
            Which.CUSTOM(ck.id),
            label = ck.label,
            scanCode = ck.scanCode,
            toggle = ck.toggle,
        ))
        val added = customList.last()
        clampToPreview(added)
        saveCustomAll()
        selected = added.which
        onSelectionChanged?.invoke(added.which, added.scale)
        onChanged?.invoke()
        invalidate()
    }

    fun removeCustomKey(id: String) {
        val idx = customList.indexOfFirst { (it.which as? Which.CUSTOM)?.id == id }
        if (idx < 0) return
        customList.removeAt(idx)
        if ((selected as? Which.CUSTOM)?.id == id) {
            selected = Which.JOYSTICK
            onSelectionChanged?.invoke(selected, selectedScale())
        }
        saveCustomAll()
        onChanged?.invoke()
        invalidate()
    }

    fun updateCustomKey(id: String, label: String, scanCode: Int, toggle: Boolean) {
        val b = customList.firstOrNull { (it.which as? Which.CUSTOM)?.id == id } ?: return
        b.label = label
        b.scanCode = scanCode
        b.toggle = toggle
        saveCustomAll()
        invalidate()
    }

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

    fun customKeys(): List<SettingsActivity.CustomKey> = customList.map { b ->
        SettingsActivity.CustomKey(
            id = (b.which as Which.CUSTOM).id,
            label = b.label,
            scanCode = b.scanCode,
            xFrac = b.xFrac,
            yFrac = b.yFrac,
            scale = b.scale,
            toggle = b.toggle,
        )
    }

    fun resetAll() {
        joystick.xFrac = SettingsActivity.DEFAULT_JOYSTICK_X
        joystick.yFrac = SettingsActivity.DEFAULT_JOYSTICK_Y
        joystick.scale = 1f
        jump.xFrac = SettingsActivity.DEFAULT_JUMP_X
        jump.yFrac = SettingsActivity.DEFAULT_JUMP_Y
        jump.scale = 1f
        ime.xFrac = SettingsActivity.DEFAULT_IME_X
        ime.yFrac = SettingsActivity.DEFAULT_IME_Y
        ime.scale = 1f
        itemBar.xFrac = SettingsActivity.DEFAULT_ITEMBAR_X
        itemBar.yFrac = SettingsActivity.DEFAULT_ITEMBAR_Y
        itemBar.scale = 1f
        sprint.xFrac = SettingsActivity.DEFAULT_SPRINT_X
        sprint.yFrac = SettingsActivity.DEFAULT_SPRINT_Y
        sprint.scale = 1f
        for (b in fixedButtons) {
            clampToPreview(b)
            save(b)
        }
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
        SettingsActivity.saveCustomKeys(context, customKeys())
    }
}
