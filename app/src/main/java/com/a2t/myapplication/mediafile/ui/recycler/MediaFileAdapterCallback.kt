package com.a2t.myapplication.mediafile.ui.recycler

import com.a2t.myapplication.mediafile.presentation.MediaFileViewModel

interface MediaFileAdapterCallback {
    fun getVM (): MediaFileViewModel
}