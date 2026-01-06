package com.a2t.myapplication.mediafile.domaim.api

import android.net.Uri
import com.a2t.myapplication.mediafile.data.dto.Response
import com.a2t.myapplication.mediafile.data.model.MediaType
import com.a2t.myapplication.mediafile.domaim.model.MediaItem
import java.io.File

interface MediaFileInteractor {

    fun getAllMediaFiles(type: MediaType): List<MediaItem>                          // Получение списка имеющихся в общем хранилище медиафайлов
    fun saveImageToPrivateStorage(sourceUri: Uri, mediaFileType: String, deleteSourceAfterSave: Boolean): Response  // Сохранение медиафайла из общего хранилища во внутреннем
    fun getFileUri(fileName: String): Uri?                                          // Получение URI файла во внутреннем хранилище
    fun addMediaFile(id: Long, fileName: String)                                    // Прикрепить медиафайл к строке
    fun deleteMediaFile (id: Long)                                                  // Открепить медиафайл от строки

    suspend fun addPhotoToGallery(file: File): Uri?
    suspend fun addVideoToGallery(file: File): Uri?
}