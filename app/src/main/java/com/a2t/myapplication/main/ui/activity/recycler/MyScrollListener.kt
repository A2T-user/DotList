package com.a2t.myapplication.main.ui.activity.recycler

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.a2t.myapplication.main.ui.activity.recycler.model.ScrollState

class MyScrollListener(private val listener: OnScrollStateChangedListener) : RecyclerView.OnScrollListener() {
    private var scrollState: ScrollState = ScrollState.STOPPED

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)

        scrollState = when {
            dy > 0 -> ScrollState.DOWN // Прокрутка вниз
            dy < 0 -> ScrollState.UP // Прокрутка вверх
            else -> ScrollState.STOPPED // Прокрутка остановлена
        }

        val layoutManager = recyclerView.layoutManager as LinearLayoutManager
        val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
        val itemCount = layoutManager.itemCount

        if (lastVisibleItemPosition == itemCount - 1) {
            scrollState = ScrollState.END // Убедитесь, что это срабатывает
        }

        listener.onScrollStateChanged(scrollState)
    }
}