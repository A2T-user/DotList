package com.a2t.myapplication.utilities

import android.app.Activity
import android.util.Log
import android.view.View

class AppHelper {
    companion object {
        fun requestFocusInTouch(view: View, ma: Activity) {
            view.isFocusableInTouchMode = true
            view.requestFocus()
            Log.e("МОЁ", "Текущий объект с фокусом: ${ma.resources.getResourceEntryName(view.id)}")
            view.isFocusableInTouchMode = false
        }

        fun animationShowAlpha(view: View, duration: Long) {
            view.visibility = View.VISIBLE // Делаем видимым перед анимацией
            view.alpha = 0f     // Начальное значение alpha
            view.scaleX = 0f    // Начальный масштаб по оси X
            view.scaleY = 0f    // Начальный масштаб по оси Y
            view.animate()
                .alpha(1f) // Конечное значение alpha
                .scaleX(1f) // Конечное значение масштаба по оси X
                .scaleY(1f) // Конечное значение масштаба по оси Y
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