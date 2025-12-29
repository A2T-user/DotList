package com.a2t.myapplication.mediafile.data.dto

import android.net.Uri

data class MediaItemDto(
    val uri: Uri,                   // Uri файла
    val creationTime: Long,         // Время создания милисек
    val mediaFileFormat: String,    // Формат файла
    val mediaFileType: String       // Тип файла I - image, V - video
)
