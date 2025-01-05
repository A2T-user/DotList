package com.a2t.myapplication.description.ui

import android.view.GestureDetector
import android.view.MotionEvent
import kotlin.math.abs

class SwipeGestureListener(private val listener: OnSwipeListener) : GestureDetector.SimpleOnGestureListener() {
    companion object {
        private const val SWIPE_THRESHOLD = 10
        private const val SWIPE_VELOCITY_THRESHOLD = 100
    }

    override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
        if (e1 == null) return false

        val diffX = e2.x - e1.x
        val diffY = e2.y - e1.y
        return if (abs(diffX) > abs(diffY)) {
            if (abs(diffX) > SWIPE_THRESHOLD && abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                when {
                    diffX < 0 -> {
                        listener.onSwipeLeft()
                        true
                    }
                    diffX > 0 -> {
                        listener.onSwipeRight()
                        true
                    }
                    else -> false
                }
            } else false
        } else false
    }

    interface OnSwipeListener {
        fun onSwipeLeft()
        fun onSwipeRight()
    }
}