package com.a2t.myapplication.mediafile.domaim.api

import android.net.Uri
import com.a2t.myapplication.mediafile.data.dto.DirType
import com.a2t.myapplication.mediafile.data.dto.MediaFileType
import com.a2t.myapplication.mediafile.data.dto.Response
import com.a2t.myapplication.mediafile.domaim.model.MediaItem

interface MediaFileInteractor {

    fun getAllMediaFiles(): List<MediaItem>                          // Получение списка имеющихся в общем хранилище медиафайлов
    // Сохранение медиафайла из общего хранилища во внешнее хранилище приложения
    fun saveFileToExternalAppStorage(sourceUri: Uri, mediaFileType: MediaFileType, dir: DirType, isNewCopy: Boolean): Response
    fun updateMediaFile(id: Long, fileName: String?)                                 // Обновляем медиафайл
    // Сохранение медиафайла из внешнего хранилища приложения в общем хранилище
    suspend fun copyFileFromExternalAppToPublicStorage(sourceUri: Uri, mediaFileType: MediaFileType): Response
}