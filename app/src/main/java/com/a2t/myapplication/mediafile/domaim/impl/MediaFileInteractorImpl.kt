package com.a2t.myapplication.mediafile.domaim.impl

import android.net.Uri
import com.a2t.myapplication.mediafile.data.MediaFileConverter
import com.a2t.myapplication.mediafile.data.dto.DirType
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
    // Сохранение медиафайла из общего хранилища во внешнее хранилище приложения
    override fun saveFileToExternalAppStorage(sourceUri: Uri, mediaFileType: MediaFileType, dir: DirType, isNewCopy: Boolean): Response {
        return storagesRepository.saveFileToExternalAppStorage(sourceUri, mediaFileType, dir, isNewCopy)
    }
    // Прикрепить/открепить медиафайл к строке
    override fun updateMediaFile(id: Long, fileName: String?) {
        mediaFileDBRepositori.updateMediaFile(id, fileName)
    }
    // Сохранение медиафайла из внешнего хранилища приложения в общем хранилище
    override suspend fun copyFileFromExternalAppToPublicStorage(sourceUri: Uri, mediaFileType: MediaFileType): Response {
        return storagesRepository.copyFileFromExternalAppToPublicStorage(sourceUri, mediaFileType)
    }

}