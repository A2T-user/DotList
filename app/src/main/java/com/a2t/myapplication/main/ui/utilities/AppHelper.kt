package com.a2t.myapplication.main.ui.utilities

import android.view.View

class AppHelper {
    companion object {
        fun requestFocusInTouch(view: View) {
            view.isFocusableInTouchMode = true
            view.requestFocus()
            view.isFocusableInTouchMode = false
        }
    }
}