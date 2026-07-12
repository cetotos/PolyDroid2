package com.cetotos.polydroid2

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import android.widget.SeekBar
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlin.math.roundToInt

class OverlayEditorActivity : AppCompatActivity() {

    private lateinit var editor: OverlayEditorView
    private lateinit var panelScroll: HorizontalScrollView

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        val root = object : FrameLayout(this) {
            private var routeToEditor = false
            override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean {
                if (ev.actionMasked == android.view.MotionEvent.ACTION_DOWN) {
                    val overPanel = ::panelScroll.isInitialized && panelScroll.visibility == View.VISIBLE &&
                        ev.x >= panelScroll.x && ev.x <= panelScroll.x + panelScroll.width &&
                        ev.y >= panelScroll.y && ev.y <= panelScroll.y + panelScroll.height
                    routeToEditor = !overPanel && ::editor.isInitialized && editor.hasButtonAt(ev.x, ev.y)
                }
                if (routeToEditor) {
                    val handled = editor.dispatchTouchEvent(ev)
                    if (ev.actionMasked == android.view.MotionEvent.ACTION_UP ||
                        ev.actionMasked == android.view.MotionEvent.ACTION_CANCEL) {
                        routeToEditor = false
                    }
                    return handled
                }
                return super.dispatchTouchEvent(ev)
            }
        }

        editor = OverlayEditorView(this)
        root.addView(editor, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))

        val barBg = android.graphics.drawable.GradientDrawable().apply {
            setColor(Color.argb(215, 35, 35, 40))
            cornerRadius = dp(14).toFloat()
        }

        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = barBg
            setPadding(dp(10), dp(2), dp(10), dp(2))
        }

        val backBtn = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = "Back"
            setOnClickListener { finish() }
        }
        topBar.addView(backBtn)

        val modeGroup = MaterialButtonToggleGroup(this).apply {
            isSingleSelection = true
            isSelectionRequired = true
        }
        val mode1Btn = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = "1.0"
            id = View.generateViewId()
        }
        val mode2Btn = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = "2.0"
            id = View.generateViewId()
        }
        modeGroup.addView(mode1Btn)
        modeGroup.addView(mode2Btn)
        modeGroup.check(mode1Btn.id)
        topBar.addView(modeGroup, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { marginStart = dp(14) })

        val addBtn = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = "Add key"
        }
        topBar.addView(addBtn, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { marginStart = dp(14) })

        val resetBtn = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = "Reset"
        }
        topBar.addView(resetBtn, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { marginStart = dp(8) })

        root.addView(topBar, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            topMargin = dp(10)
            marginStart = dp(10)
        })

        val panelRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), 0, dp(14), 0)
        }
        panelScroll = HorizontalScrollView(this).apply {
            background = barBg.constantState!!.newDrawable().mutate()
            isHorizontalScrollBarEnabled = false
            addView(panelRow, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.MATCH_PARENT
            ))
            visibility = View.GONE
        }
        root.addView(panelScroll, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT, dp(65)
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            bottomMargin = dp(10)
        })

        fun rowParams(w: Int = LinearLayout.LayoutParams.WRAP_CONTENT) =
            LinearLayout.LayoutParams(w, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                marginStart = dp(8)
            }

        val nameLabel = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 14f
        }
        panelRow.addView(nameLabel, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        var suppressLabelEdits = false
        val labelEdit = TextInputEditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            imeOptions = EditorInfo.IME_ACTION_DONE
            maxLines = 1
            addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    if (!suppressLabelEdits) editor.setSelectedLabel(s?.toString() ?: "")
                }
            })
        }
        val labelEditLayout = TextInputLayout(this).apply {
            hint = "Text"
            addView(labelEdit)
        }
        panelRow.addView(labelEditLayout, rowParams(dp(110)))

        fun dropdown(hintText: String, options: List<String>, width: Int): Pair<AutoCompleteTextView, TextInputLayout> {
            val field = AutoCompleteTextView(this).apply {
                inputType = InputType.TYPE_NULL
                setAdapter(ArrayAdapter(
                    this@OverlayEditorActivity,
                    android.R.layout.simple_dropdown_item_1line,
                    options,
                ))
            }
            val fieldLayout = TextInputLayout(
                this, null,
                com.google.android.material.R.attr.textInputOutlinedExposedDropdownMenuStyle
            ).apply {
                hint = hintText
                endIconMode = TextInputLayout.END_ICON_DROPDOWN_MENU
                addView(field)
            }
            panelRow.addView(fieldLayout, rowParams(width))
            return field to fieldLayout
        }

        val (keyDropdown, keyDropdownLayout) = dropdown("Key", SettingsActivity.KEY_OPTIONS.map { it.first }, dp(104))
        val (colorDropdown, colorDropdownLayout) = dropdown("Color", SettingsActivity.KEY_COLOR_OPTIONS.map { it.first }, dp(118))
        val (shapeDropdown, shapeDropdownLayout) = dropdown("Shape", SettingsActivity.KEY_SHAPE_OPTIONS.map { it.second }, dp(112))

        val sizeLabel = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 12f
        }
        panelRow.addView(sizeLabel, rowParams())
        @SuppressLint("ClickableViewAccessibility")
        fun noScrollSteal(sb: SeekBar) {
            sb.setOnTouchListener { v, ev ->
                when (ev.actionMasked) {
                    android.view.MotionEvent.ACTION_DOWN ->
                        v.parent.requestDisallowInterceptTouchEvent(true)
                    android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL ->
                        v.parent.requestDisallowInterceptTouchEvent(false)
                }
                false
            }
        }

        val sizeSlider = SeekBar(this).apply { max = 30 }
        noScrollSteal(sizeSlider)
        panelRow.addView(sizeSlider, rowParams(dp(140)))

        val textSizeLabel = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 12f
        }
        panelRow.addView(textSizeLabel, rowParams())
        val textSizeSlider = SeekBar(this).apply { max = 30 }
        noScrollSteal(textSizeSlider)
        panelRow.addView(textSizeSlider, rowParams(dp(140)))

        val toggleSwitch = MaterialSwitch(this).apply {
            text = "Toggle"
        }
        panelRow.addView(toggleSwitch, rowParams())

        val cloneBtn = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = "Clone"
        }
        panelRow.addView(cloneBtn, rowParams())

        val removeBtn = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = "Remove"
        }
        panelRow.addView(removeBtn, rowParams())

        fun whichName(which: OverlayEditorView.Which?): String? = when (which) {
            OverlayEditorView.Which.JOYSTICK -> "Joystick"
            OverlayEditorView.Which.JUMP -> "Jump"
            OverlayEditorView.Which.IME -> "Keyboard"
            OverlayEditorView.Which.ITEMBAR -> "Item bar"
            OverlayEditorView.Which.SPRINT -> "Sprint"
            is OverlayEditorView.Which.CUSTOM -> "Custom key"
            null -> null
        }

        fun refreshPanel() {
            val which = editor.selected
            val name = whichName(which)
            if (name == null) {
                panelScroll.visibility = View.GONE
                return
            }
            panelScroll.visibility = View.VISIBLE
            nameLabel.text = name
            sizeLabel.text = "Size ${"%.2f".format(editor.selectedScale())}x"
            sizeSlider.progress = (((editor.selectedScale() - 0.5f) / 0.05f).roundToInt()).coerceIn(0, sizeSlider.max)

            val t = editor.selectedToggle()
            toggleSwitch.visibility = if (t != null) View.VISIBLE else View.GONE
            if (t != null) {
                toggleSwitch.setOnCheckedChangeListener(null)
                toggleSwitch.isChecked = t
                toggleSwitch.setOnCheckedChangeListener { _, checked -> editor.setSelectedToggle(checked) }
            }

            val ck = editor.selectedCustom()
            val customVis = if (ck != null) View.VISIBLE else View.GONE
            labelEditLayout.visibility = customVis
            keyDropdownLayout.visibility = customVis
            colorDropdownLayout.visibility = customVis
            shapeDropdownLayout.visibility = customVis
            textSizeLabel.visibility = customVis
            textSizeSlider.visibility = customVis
            cloneBtn.visibility = customVis
            removeBtn.visibility = customVis
            if (ck != null) {
                suppressLabelEdits = true
                if (labelEdit.text?.toString() != ck.label) labelEdit.setText(ck.label)
                suppressLabelEdits = false
                keyDropdown.setText(SettingsActivity.KEY_OPTIONS.firstOrNull { it.second == ck.scanCode }?.first ?: "", false)
                colorDropdown.setText(SettingsActivity.KEY_COLOR_OPTIONS.firstOrNull { it.second == ck.color }?.first ?: "Custom", false)
                shapeDropdown.setText(SettingsActivity.KEY_SHAPE_OPTIONS.firstOrNull { it.first == ck.shape }?.second ?: "Circle", false)
                textSizeLabel.text = "Text ${"%.2f".format(ck.textScale)}x"
                textSizeSlider.progress = (((ck.textScale - 0.5f) / 0.05f).roundToInt()).coerceIn(0, textSizeSlider.max)
            }
        }

        editor.onSelectionChanged = { _, _ -> refreshPanel() }

        editor.onDragStateChanged = { dragging ->
            val a = if (dragging) 0.08f else 1f
            topBar.animate().alpha(a).setDuration(120).start()
            panelScroll.animate().alpha(a).setDuration(120).start()
        }

        modeGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                editor.setMode(checkedId == mode2Btn.id)
                refreshPanel()
            }
        }

        sizeSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                val value = 0.5f + progress * 0.05f
                editor.setSelectedScale(value)
                sizeLabel.text = "Size ${"%.2f".format(value)}x"
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        textSizeSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                val value = 0.5f + progress * 0.05f
                editor.setSelectedTextScale(value)
                textSizeLabel.text = "Text ${"%.2f".format(value)}x"
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        keyDropdown.setOnItemClickListener { _, _, position, _ ->
            val ck = editor.selectedCustom() ?: return@setOnItemClickListener
            val (keyName, scan) = SettingsActivity.KEY_OPTIONS[position]
            val oldKeyName = SettingsActivity.KEY_OPTIONS.firstOrNull { it.second == ck.scanCode }?.first
            editor.setSelectedScan(scan)
            if (ck.label.isEmpty() || ck.label == oldKeyName) {
                editor.setSelectedLabel(keyName)
                suppressLabelEdits = true
                labelEdit.setText(keyName)
                suppressLabelEdits = false
            }
        }

        colorDropdown.setOnItemClickListener { _, _, position, _ ->
            editor.setSelectedColor(SettingsActivity.KEY_COLOR_OPTIONS[position].second)
        }

        shapeDropdown.setOnItemClickListener { _, _, position, _ ->
            editor.setSelectedShape(SettingsActivity.KEY_SHAPE_OPTIONS[position].first)
        }

        addBtn.setOnClickListener {
            val (keyName, scan) = SettingsActivity.KEY_OPTIONS[0]
            editor.addCustomKey(SettingsActivity.CustomKey(
                id = "ck_${System.currentTimeMillis()}",
                label = keyName, scanCode = scan,
                xFrac = 0.5f, yFrac = 0.5f, scale = 1f, toggle = false,
            ))
            refreshPanel()
        }

        cloneBtn.setOnClickListener {
            editor.cloneSelected()
            refreshPanel()
        }

        removeBtn.setOnClickListener {
            val cid = editor.selectedCustomId() ?: return@setOnClickListener
            editor.removeCustomKey(cid)
            refreshPanel()
        }

        resetBtn.setOnClickListener {
            com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Reset overlay layout?")
                .setMessage(if (editor.v2)
                    "This removes all custom keys you added for 2.0."
                else
                    "This puts the joystick and buttons back to their default positions and sizes, and removes any custom keys you added.")
                .setPositiveButton("Reset") { _, _ ->
                    editor.resetAll()
                    refreshPanel()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        refreshPanel()
        setContentView(root)
    }
}
