package com.a2t.myapplication.main.ui.recycler

import com.a2t.myapplication.main.ui.recycler.model.ScrollState

interface OnScrollStateChangedListener {
    fun onScrollStateChanged(scrollState: ScrollState)
}