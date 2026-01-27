package com.a2t.myapplication.mediafile.domaim.api

interface MediaViewerInteractor {
    fun updateMediaFile(id: Long, fileName: String?) // Обновляем медиафайл
    fun updateRecordAndNote (id: Long, record: String, note: String) // Обновляем поля Основное и примечание
}