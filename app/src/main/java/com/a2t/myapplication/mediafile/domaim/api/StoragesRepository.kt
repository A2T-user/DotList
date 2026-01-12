package com.a2t.myapplication.mediafile.domaim.api

import android.net.Uri
import com.a2t.myapplication.mediafile.data.dto.MediaFileType
import com.a2t.myapplication.mediafile.data.dto.MediaItemDto
import com.a2t.myapplication.mediafile.data.dto.Response

interface StoragesRepository {
    fun getAllMediaFiles(): List<MediaItemDto>
    fun saveImageToPrivateStorage(sourceUri: Uri, mediaFileType: MediaFileType, deleteSourceAfterSave: Boolean, isNewCopy: Boolean): Response
    fun getFileUri(fileName: String): Uri?
}