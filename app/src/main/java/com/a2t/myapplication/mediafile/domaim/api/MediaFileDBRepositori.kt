package com.a2t.myapplication.mediafile.domaim.api

interface MediaFileDBRepositori {

    fun addMediaFile(id: Long, fileName: String)    // Прикрепить медиафайл к строке

    fun deleteMediaFile (id: Long)                  // Открепить медиафайл от строки
}