package com.a2t.myapplication.mediafile.domaim.api

import android.net.Uri
import com.a2t.myapplication.mediafile.data.dto.MediaItemDto
import com.a2t.myapplication.mediafile.data.model.MediaType

interface StoragesRepository {
    fun getAllMediaFiles(type: MediaType): List<MediaItemDto>
    fun saveImageToPrivateStorage(sourceUri: Uri): String?
    fun getFileUri(fileName: String): Uri?
}