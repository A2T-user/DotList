package com.a2t.myapplication.description.ui

import android.view.GestureDetector
import android.view.MotionEvent
import kotlin.math.abs

@Suppress("SameReturnValue")
class SwipeGestureListener(private val listener: OnSwipeListener) : GestureDetector.SimpleOnGestureListener() {
    companion object {
        private const val SWIPE_THRESHOLD = -30
    }

    override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
        if (e1 == null) {
            return false
        }
        val diffX = e2.x - e1.x
        val diffY = e2.y - e1.y
        return if (abs(diffX / diffY) > 1) {
            if (diffX < SWIPE_THRESHOLD) listener.onSwipeLeft() else true
        } else false
    }

    interface OnSwipeListener {
        fun onSwipeLeft(): Boolean
    }
}