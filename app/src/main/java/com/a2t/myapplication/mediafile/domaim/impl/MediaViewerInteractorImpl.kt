package com.a2t.myapplication.mediafile.domaim.impl

import com.a2t.myapplication.mediafile.domaim.api.MediaFileDBRepositori
import com.a2t.myapplication.mediafile.domaim.api.MediaViewerInteractor

class MediaViewerInteractorImpl(
    private val mediaFileDBRepositori: MediaFileDBRepositori
): MediaViewerInteractor {
    // Обновляем медиафайл
    override fun updateMediaFile(id: Long, fileName: String?) {
        mediaFileDBRepositori.updateMediaFile(id, fileName)
    }

    // Обновляем поля Основное и примечание
    override fun updateRecordAndNote(id: Long, record: String, note: String) {
        mediaFileDBRepositori.updateRecordAndNote(id, record, note)
    }
}