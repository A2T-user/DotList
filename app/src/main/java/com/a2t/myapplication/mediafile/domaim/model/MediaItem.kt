package com.a2t.myapplication.mediafile.domaim.model

import android.net.Uri
import com.a2t.myapplication.mediafile.data.dto.DirType
import com.a2t.myapplication.mediafile.data.dto.MediaFileType

data class MediaItem(
    val uri: Uri,                   // Uri файла
    val creationDate: String,       // Время создания "dd.mm.yy"
    val mediaFileType: MediaFileType,
    var dir: DirType                // Тип папки
)
