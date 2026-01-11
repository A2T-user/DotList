package com.a2t.myapplication.mediafile.domaim.api

import android.net.Uri
import com.a2t.myapplication.mediafile.data.dto.MediaFileType
import com.a2t.myapplication.mediafile.data.dto.Response
import com.a2t.myapplication.mediafile.domaim.model.MediaItem
import java.io.File

interface MediaFileInteractor {

    fun getAllMediaFiles(): List<MediaItem>                          // Получение списка имеющихся в общем хранилище медиафайлов
    // Сохранение медиафайла из общего хранилища во внутреннем
    fun saveImageToPrivateStorage(sourceUri: Uri, mediaFileType: MediaFileType, deleteSourceAfterSave: Boolean, isNewCopy: Boolean): Response
    fun getFileUri(fileName: String): Uri?                                          // Получение URI файла во внутреннем хранилище
    fun updateMediaFile(id: Long, fileName: String)                                 // Обновляем медиафайл
    suspend fun addPhotoToGallery(file: File): Uri?
    suspend fun addVideoToGallery(file: File): Uri?
}