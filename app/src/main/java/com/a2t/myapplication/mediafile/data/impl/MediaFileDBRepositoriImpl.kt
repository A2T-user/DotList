package com.a2t.myapplication.mediafile.data.impl

import com.a2t.myapplication.common.data.AppDatabase
import com.a2t.myapplication.mediafile.domaim.api.MediaFileDBRepositori

class MediaFileDBRepositoriImpl(
    private val appDatabase: AppDatabase
): MediaFileDBRepositori {

    // Обновляем медиафайл
    override fun updateMediaFile(id: Long, fileName: String?) {
        val time = System.currentTimeMillis()
        appDatabase.mediaFileDao().updateMediaFile(id, fileName, time)
    }

    // Обновляем поля Основное и примечание
    override fun updateRecordAndNote(
        id: Long,
        record: String,
        note: String
    ) {
        val time = System.currentTimeMillis()
        appDatabase.mediaFileDao().updateRecordAndNote(id, record, note, time)
    }
}