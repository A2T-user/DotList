package com.a2t.myapplication.common.utilities

import android.net.Uri
import android.provider.OpenableColumns
import android.view.View
import com.a2t.myapplication.common.App
import java.io.File

class AppHelper {
    companion object {
        // Передает на мгновение фокус объекту view
        fun requestFocusInTouch(view: View) {
            view.isFocusableInTouchMode = true
            view.requestFocus()
            view.isFocusableInTouchMode = false
        }

        // Получение имени файла из Uri
        fun getFileNameFromUri(uri: Uri): String? {
            return when (uri.scheme) {
                "content" -> {
                    App.appContext.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1 && cursor.moveToFirst()) {
                            cursor.getString(nameIndex)
                        } else {
                            null
                        }
                    }
                }
                "file" -> File(uri.path!!).name
                else -> uri.lastPathSegment
            }
        }

        // Получение MimeType из имени файла
        fun getMimeType(fileName: String): String {
            val extension = fileName.substringAfterLast(".", "").lowercase()
            return when (extension) {
                // Изображения
                "jpg", "jpeg" -> "image/jpeg"
                "png" -> "image/png"
                "gif" -> "image/gif"
                "webp" -> "image/webp"
                "bmp" -> "image/bmp"
                "heic" -> "image/heic"
                "heif" -> "image/heif"
                // Видео
                "mp4" -> "video/mp4"
                "mov" -> "video/quicktime"
                "3gp" -> "video/3gpp"
                "3g2" -> "video/3gpp2"
                "avi" -> "video/x-msvideo"
                "flv" -> "video/x-flv"
                "mkv" -> "video/x-matroska"
                "webm" -> "video/webm"
                "m4v" -> "video/x-m4v"
                "ogv" -> "video/ogg"
                "asf" -> "video/x-ms-asf"
                "wmv" -> "video/x-ms-wmv"
                "mpg", "mpeg" -> "video/mpeg"
                "ts" -> "video/mp2t"
                "mts" -> "video/mp2t"
                "dv" -> "video/x-dv"
                else -> "*/*"   // По умолчанию
            }
        }
    }
}