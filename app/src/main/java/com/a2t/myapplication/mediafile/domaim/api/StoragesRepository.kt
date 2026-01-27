package com.a2t.myapplication.mediafile.domaim.api

import android.net.Uri
import com.a2t.myapplication.mediafile.data.dto.DirType
import com.a2t.myapplication.mediafile.data.dto.MediaFileType
import com.a2t.myapplication.mediafile.data.dto.MediaItemDto
import com.a2t.myapplication.mediafile.data.dto.Response

interface StoragesRepository {
    fun getAllMediaFiles(): List<MediaItemDto>
    // Сохранение медиафайла из общего хранилища во внешнее хранилище приложения
    fun saveFileToExternalAppStorage(sourceUri: Uri, mediaFileType: MediaFileType, dir: DirType, isNewCopy: Boolean): Response
    // Сохранение медиафайла из внешнего хранилища приложения в общем хранилище
    suspend fun copyFileFromExternalAppToPublicStorage(sourceUri: Uri, mediaFileType: MediaFileType): Response
}