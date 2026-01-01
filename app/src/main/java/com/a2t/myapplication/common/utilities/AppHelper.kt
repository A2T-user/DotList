package com.a2t.myapplication.common.utilities

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.view.View

class AppHelper {
    companion object {
        // Передает на мгновение фокус объекту view
        fun requestFocusInTouch(view: View) {
            view.isFocusableInTouchMode = true
            view.requestFocus()
            view.isFocusableInTouchMode = false
        }
        // Проверяем лежит ли файл с этим uri в папке Загрузки
        fun isFileInDownloadsFolder(context: Context, uri: Uri): Boolean {
            val path = getFilePathFromUri(context, uri)?.toString() ?: return false
            // Проверяем наличие "Download" или "Загрузки" в пути (без учёта регистра)
            return path.contains("Download", ignoreCase = true) || path.contains("Загрузки", ignoreCase = true)
        }
        // Функция для извлечения пути из URI
        fun getFilePathFromUri(context: Context, uri: Uri): Any? {
            if (uri.scheme == "content") {
                val projection = arrayOf(MediaStore.MediaColumns.DATA)
                context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        return cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA))
                    }
                }
            } else if (uri.scheme == "file") {
                return uri.path
            }
            return null
        }
    }
}