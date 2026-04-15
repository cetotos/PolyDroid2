package com.cetotos.polydroid2

import android.content.Context
import android.text.Editable
import android.text.Selection
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.widget.AppCompatEditText

/**
 * IME capture, based on PojavLauncher / ZalithLauncher TouchCharInput.
 * Hidden EditText prefilled with filler; TextWatcher fires for each typed char.
 * Each char is translated to KeyEvents via KeyCharacterMap and dispatched to [target]
 * so it takes the same path as physical keys (GameActivity.dispatchKeyEvent -> nativeSendKeyEvent).
 */
class ImeCaptureView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
) : AppCompatEditText(context, attrs) {

    companion object {
        private const val FILLER = "                              "
    }

    private var internalChange = false
    var sendKey: ((scanCode: Int, keyCode: Int, down: Boolean) -> Unit)? = null

    init {
        isFocusable = true
        isFocusableInTouchMode = true
        addTextChangedListener(Watcher())
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
        for (e in events) {
            cb(e.scanCode, e.keyCode, e.action == KeyEvent.ACTION_DOWN)
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
