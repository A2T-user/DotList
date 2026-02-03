package com.a2t.myapplication.common.utilities

import android.net.Uri
import com.a2t.myapplication.common.App
import java.io.File

class FileValidator {

    private companion object {
        // Все сигнатуры в одном месте — изображения + видео + аудио
        private val MIME_SIGNATURES = mapOf(
            // Images
            "FFD8FF" to "image/jpeg",
            "89504E47" to "image/png",
            "52494646" to "image/webp",
            "424D" to "image/bmp",
            "6674797068656963" to "image/heic",  // HEIC
            "6674797068656978" to "image/heic",  // HEIX
            "667479706d696631" to "image/heic",  // HEIF (mif1)
            "6674797061766966" to "image/avif",  // AVIF (corrected)

            // Videos
            "000000206674797069736F6D" to "video/mp4",
            "000000146674797069736F6D" to "video/mp4",
            "52494646" to "video/webm",
            "2142444E" to "video/quicktime",
        )

        private const val SIGNATURE_LENGTH = 16
    }

    fun validatePublicStorageFile(uri: Uri): Boolean = validateFile(uri, true)
    fun validateExternalAppStorageFile(path: String): Boolean = validateFile(path, false)

    private fun validateFile(source: Any, isContentUri: Boolean): Boolean {
        try {
            // Проверяем размер
            val fileSize = getFileSize(source, isContentUri)
            if (fileSize <= 0) return false
            // Читаем сигнатуру
            val signature = readFirstBytes(source, isContentUri)
            if (signature.isEmpty()) return false
            // Конвертируем в HEX
            val hexSignature = signature.joinToString("") { "%02X".format(it) }
            // Определяем MIME-тип
            val mimeType = MIME_SIGNATURES.entries
                .find { hexSignature.startsWith(it.key) }
                ?.value ?: return false
            // Проверяем, что тип поддерживается (image/video)
            return mimeType.startsWith("image/") || mimeType.startsWith("video/")
        } catch (_: Exception) {
            return false
        }
    }

    private fun getFileSize(source: Any, isContentUri: Boolean): Long {
        return if (isContentUri) {
            val uri = source as Uri
            App.appContext.contentResolver.query(uri, arrayOf(android.provider.MediaStore.Files.FileColumns.SIZE), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getLong(0) else 0L
            } ?: 0L
        } else {
            val file = File(source as String)
            if (file.exists()) file.length() else 0L
        }
    }

    private fun readFirstBytes(source: Any, isContentUri: Boolean): ByteArray {
        return if (isContentUri) {
            val uri = source as Uri
            App.appContext.contentResolver.openInputStream(uri)?.use {
                it.readBytes().take(SIGNATURE_LENGTH).toByteArray()
            } ?: byteArrayOf()
        } else {
            val file = File(source as String)
            if (!file.exists()) byteArrayOf()
            else file.inputStream().use { it.readBytes().take(SIGNATURE_LENGTH).toByteArray() }
        }
    }
}
