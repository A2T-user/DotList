package com.a2t.myapplication.main.ui

import android.content.Context
import android.content.res.Configuration
import android.util.AttributeSet
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.appcompat.widget.AppCompatEditText

class ActionEditText : AppCompatEditText {
    constructor(context: Context?) : super(context!!)
    constructor(context: Context?, attrs: AttributeSet?) : super(
        context!!, attrs
    )

    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
        context!!, attrs, defStyle
    )

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            imeOptions =
                EditorInfo.IME_ACTION_DONE or EditorInfo.IME_FLAG_NO_EXTRACT_UI // В положении ландшафт, отменить полноэкранный режим клавиатуры
        }
        val conn = super.onCreateInputConnection(outAttrs)
        outAttrs.imeOptions = outAttrs.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION.inv()
        return conn
    }
}