package com.a2t.myapplication.mediafile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a2t.myapplication.mediafile.data.model.MediaType
import com.a2t.myapplication.mediafile.domaim.api.MediaFileInteractor
import com.a2t.myapplication.mediafile.domaim.model.MediaItem
import kotlinx.coroutines.launch

class MediaFileViewModel(
    private val mediaFileInteractor: MediaFileInteractor
): ViewModel() {
    fun getAllMediaFiles(type: MediaType, callback: (List<MediaItem>) -> Unit) {
        viewModelScope.launch {
            val itemList = mediaFileInteractor.getAllMediaFiles(type)
            callback(itemList)
        }
    }
}