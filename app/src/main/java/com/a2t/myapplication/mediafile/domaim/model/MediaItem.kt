package com.a2t.myapplication.mediafile.domaim.model

import android.net.Uri

data class MediaItem(
    val uri: Uri,                   // Uri файла
    val creationDate: String,       // Время создания "dd.mm.yy"
    val mediaFileType: String       // Тип файла I - image, V - video
)
