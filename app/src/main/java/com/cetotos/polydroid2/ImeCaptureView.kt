package com.cetotos.polydroid2

import android.content.Context
import android.text.Editable
import android.text.InputType
import android.text.Selection
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.widget.AppCompatEditText

class ImeCaptureView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
) : AppCompatEditText(context, attrs) {

    companion object {
        private const val FILLER = "                              "
        private const val SCAN_ENTER = 28
    }

    private var internalChange = false
    var sendKey: ((scanCode: Int, keyCode: Int, down: Boolean) -> Unit)? = null
    private val kbExecutor = java.util.concurrent.Executors.newSingleThreadExecutor { r ->
        Thread(r, "polykbd").apply { isDaemon = true }
    }

    init {
        isFocusable = true
        isFocusableInTouchMode = true
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        imeOptions = EditorInfo.IME_ACTION_SEND or EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
        addTextChangedListener(Watcher())
        setOnEditorActionListener { _, actionId, event ->
            val isSubmit = actionId == EditorInfo.IME_ACTION_SEND ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    actionId == EditorInfo.IME_ACTION_GO ||
                    actionId == EditorInfo.IME_ACTION_NEXT ||
                    actionId == EditorInfo.IME_ACTION_UNSPECIFIED ||
                    (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER)
            if (isSubmit) {
                val cb = sendKey
                if (cb != null) {
                    cb(SCAN_ENTER, KeyEvent.KEYCODE_ENTER, true)
                    cb(SCAN_ENTER, KeyEvent.KEYCODE_ENTER, false)
                }
                disableKeyboard()
                true
            } else false
        }
        clearBuf()
        disable()
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if (!hasWindowFocus) disable()
    }

    override fun onKeyPreIme(keyCode: Int, event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
            disable()
        }
        return super.onKeyPreIme(keyCode, event)
    }

    fun switchKeyboardState() {
        if (hasFocus() && visibility == VISIBLE) disableKeyboard() else enableKeyboard()
    }

    fun enableKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        enable()
        imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
    }

    fun disableKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
        disable()
    }

    private fun enable() {
        isEnabled = true
        isFocusable = true
        visibility = VISIBLE
        requestFocus()
    }

    private fun disable() {
        clearBuf()
        visibility = GONE
        clearFocus()
        isEnabled = false
    }

    private fun clearBuf() {
        internalChange = true
        val e = editableText
        e?.clear()
        e?.append(FILLER)
        if (e != null) Selection.setSelection(e, FILLER.length)
        internalChange = false
    }

    private fun dispatchChar(c: Char) {
        val cb = sendKey ?: return
        val kcm = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD)
        val events = kcm.getEvents(charArrayOf(c)) ?: return
        kbExecutor.execute {
            for (e in events) {
                cb(e.scanCode, e.keyCode, e.action == KeyEvent.ACTION_DOWN)
                try { Thread.sleep(25) } catch (_: InterruptedException) {}
            }
        }
    }

    private inner class Watcher : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            if (internalChange) return
            if (count > 0) {
                var i = start
                var n = 0
                while (n < count) {
                    dispatchChar(s[i])
                    i++; n++
                }
            } else if (before > 0) {
                val cb = sendKey
                if (cb != null) repeat(before) {
                    cb(0, KeyEvent.KEYCODE_DEL, true)
                    cb(0, KeyEvent.KEYCODE_DEL, false)
                }
            }
        }
        override fun afterTextChanged(e: Editable) {
            if (internalChange) return
            if (e.length < 1) clearBuf()
        }
    }
}
