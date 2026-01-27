package com.a2t.myapplication.mediafile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a2t.myapplication.mediafile.domaim.api.MediaViewerInteractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MediaViewerViewModel (
    private val mediaViewerInteractor: MediaViewerInteractor
): ViewModel(){
    var id = 0L
    var record = ""
    var note = ""
    var mediaFileName = ""
    // Обновляем медиафайл
    fun updateMediaFile(id: Long, fileName: String?) {
        viewModelScope.launch (Dispatchers.IO) {
            mediaViewerInteractor.updateMediaFile(id, fileName)
        }
    }

    // Обновляем поля Основное и примечание
    fun updateRecordAndNote(id: Long, record: String, note: String) {
        viewModelScope.launch (Dispatchers.IO) {
            mediaViewerInteractor.updateRecordAndNote(id, record, note)
        }
    }
}