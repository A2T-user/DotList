package com.a2t.myapplication.mediafile.data.impl

import com.a2t.myapplication.common.data.AppDatabase
import com.a2t.myapplication.mediafile.domaim.api.MediaFileDBRepositori

class MediaFileDBRepositoriImpl(
    private val appDatabase: AppDatabase
): MediaFileDBRepositori {

    // Прикрепить медиафайл к строке
    override fun addMediaFile(id: Long, fileName: String) {
        appDatabase.mediaFileDao().addMediaFile(id, fileName)
    }

    // Открепить медиафайл от строки
    override fun deleteMediaFile (id: Long) {
        appDatabase.mediaFileDao().deleteMediaFile(id)
    }
}