package com.a2t.myapplication.mediafile.domaim.impl

import android.net.Uri
import com.a2t.myapplication.mediafile.data.MediaFileConverter
import com.a2t.myapplication.mediafile.data.model.MediaType
import com.a2t.myapplication.mediafile.domaim.api.MediaFileDBRepositori
import com.a2t.myapplication.mediafile.domaim.api.MediaFileInteractor
import com.a2t.myapplication.mediafile.domaim.api.StoragesRepository
import com.a2t.myapplication.mediafile.domaim.model.MediaItem

class MediaFileInteractorImpl(
    private val mediaFileConverter: MediaFileConverter,
    private val storagesRepository: StoragesRepository,
    private val mediaFileDBRepositori: MediaFileDBRepositori
): MediaFileInteractor {

    // Получение списка имеющихся в общем хранилище медиафайлов
    override fun getAllMediaFiles(type: MediaType): List<MediaItem> {
        val itemList = storagesRepository.getAllMediaFiles(type)
        return itemList.map { mediaFileConverter.map(it) }
    }
    // Сохранение медиафайла из общего хранилища во внутреннем и добавление его имени в строку
    override fun saveImageToPrivateStorage(sourceUri: Uri, idRec: Long) {
        val name = storagesRepository.saveImageToPrivateStorage(sourceUri)
        if (name != null) mediaFileDBRepositori.addMediaFile(idRec, name)
    }
    // Получение URI файла во внутреннем хранилище
    override fun getFileUri(fileName: String): Uri? {
        return storagesRepository.getFileUri(fileName)
    }
    // Открепить медиафайл от строки
    override fun deleteMediaFile(id: Long) {
        mediaFileDBRepositori.deleteMediaFile(id)
    }
}