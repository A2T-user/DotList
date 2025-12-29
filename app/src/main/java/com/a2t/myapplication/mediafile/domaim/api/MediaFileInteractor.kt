package com.a2t.myapplication.mediafile.domaim.api

import android.net.Uri
import com.a2t.myapplication.mediafile.data.model.MediaType
import com.a2t.myapplication.mediafile.domaim.model.MediaItem

interface MediaFileInteractor {

    fun getAllMediaFiles(type: MediaType): List<MediaItem>          // Получение списка имеющихся в общем хранилище медиафайлов
    fun saveImageToPrivateStorage(sourceUri: Uri, idRec: Long)      // Сохранение медиафайла из общего хранилища во внутреннем
                                                                    // и добавление его имени в строку
    fun getFileUri(fileName: String): Uri?                          // Получение URI файла во внутреннем хранилище
    fun deleteMediaFile (id: Long)                                  // Открепить медиафайл от строки
}