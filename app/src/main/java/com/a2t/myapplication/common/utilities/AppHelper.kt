package com.a2t.myapplication.common.utilities

import android.view.View

class AppHelper {
    companion object {
        // Передает на мгновение фокус объекту view
        fun requestFocusInTouch(view: View) {
            view.isFocusableInTouchMode = true
            view.requestFocus()
            view.isFocusableInTouchMode = false
        }
    }
}