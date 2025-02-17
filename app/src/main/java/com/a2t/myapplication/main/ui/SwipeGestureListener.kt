package com.a2t.myapplication.main.ui

import android.view.GestureDetector
import android.view.MotionEvent
import kotlin.math.abs

class SwipeGestureListener(private val listener: OnSwipeListener) : GestureDetector.SimpleOnGestureListener() {
    companion object {
        private const val SWIPE_THRESHOLD = -30
        private const val SWIPE_VELOCITY_THRESHOLD = 30
    }

    override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
        if (e1 == null) {
            return false
        }
        val diffX = e2.x - e1.x
        val diffY = e2.y - e1.y
        return if (abs(diffX / diffY) > 1) {
            if (diffX < SWIPE_THRESHOLD || abs(velocityX) > SWIPE_VELOCITY_THRESHOLD ) listener.onSwipeLeft() else false
        } else false
    }

    interface OnSwipeListener {
        fun onSwipeLeft(): Boolean
    }
}