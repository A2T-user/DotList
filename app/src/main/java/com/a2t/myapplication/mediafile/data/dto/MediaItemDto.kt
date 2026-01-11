package com.a2t.myapplication.mediafile.data.dto

import android.net.Uri

data class MediaItemDto(
    val uri: Uri,                       // Uri файла
    val creationTime: Long,             // Время создания милисек
    val mediaFileType: MediaFileType,   // Тип файла
    var dir: DirType                    // Тип папки
)
