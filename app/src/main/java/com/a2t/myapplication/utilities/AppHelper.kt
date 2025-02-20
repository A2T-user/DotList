package com.a2t.myapplication.utilities

import android.view.View

class AppHelper {
    companion object {
        fun requestFocusInTouch(view: View) {
            view.isFocusableInTouchMode = true
            view.requestFocus()
            view.isFocusableInTouchMode = false
        }

        fun animationShowAlpha(view: View, duration: Long) {
            view.visibility = View.VISIBLE // Делаем видимым перед анимацией
            view.alpha = 0f // Устанавливаем начальное значение alpha
            view.animate()
                .alpha(1f) // Конечное значение alpha
                .setDuration(duration) // Длительность анимации в миллисекундах
                .start()
        }

        fun animationHideAlpha(view: View, duration: Long) {
            view.animate()
                .alpha(0f) // Конечное значение alpha
                .setDuration(duration) // Длительность анимации в миллисекундах
                .withEndAction {
                    view.visibility = View.GONE // Делаем видимым перед анимацией
                    view.alpha = 1f
                }
                .start()
        }
    }
}