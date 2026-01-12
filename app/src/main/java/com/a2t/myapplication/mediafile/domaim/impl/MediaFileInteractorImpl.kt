package com.a2t.myapplication.mediafile.domaim.impl

import android.net.Uri
import com.a2t.myapplication.mediafile.data.MediaFileConverter
import com.a2t.myapplication.mediafile.data.dto.MediaFileType
import com.a2t.myapplication.mediafile.data.dto.Response
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
    override fun getAllMediaFiles(): List<MediaItem> {
        val itemList = storagesRepository.getAllMediaFiles()
        return itemList.map { mediaFileConverter.map(it) }
    }
    // Сохранение медиафайла из общего хранилища во внутреннем и добавление его имени в строку
    override fun saveImageToPrivateStorage(sourceUri: Uri, mediaFileType: MediaFileType, deleteSourceAfterSave: Boolean, isNewCopy: Boolean): Response {
        return storagesRepository.saveImageToPrivateStorage(sourceUri, mediaFileType, deleteSourceAfterSave, isNewCopy)
    }
    // Получение URI файла во внутреннем хранилище
    override fun getFileUri(fileName: String): Uri? {
        return storagesRepository.getFileUri(fileName)
    }
    // Прикрепить/открепить медиафайл к строке
    override fun updateMediaFile(id: Long, fileName: String) {
        mediaFileDBRepositori.updateMediaFile(id, fileName)
    }
}