package com.a2t.myapplication.main.ui.activity.recycler

import androidx.recyclerview.widget.RecyclerView
import com.a2t.myapplication.main.ui.activity.recycler.model.ScrollState

class MyScrollListener(private val listener: OnScrollStateChangedListener) : RecyclerView.OnScrollListener() {
    private var scrollState: ScrollState = ScrollState.STOPPED

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)

        scrollState = if (dy > 0) {
            ScrollState.DOWN // Прокрутка вниз
        } else if (dy < 0) {
            ScrollState.UP // Прокрутка вверх
        } else {
            ScrollState.STOPPED // Прокрутка остановлена
        }

        listener.onScrollStateChanged(scrollState)
    }
}