package com.a2t.myapplication.mediafile.data.impl

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.StatFs
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.core.net.toUri
import com.a2t.myapplication.R
import com.a2t.myapplication.common.App
import com.a2t.myapplication.mediafile.data.dto.MediaItemDto
import com.a2t.myapplication.mediafile.data.model.MediaType
import com.a2t.myapplication.mediafile.domaim.api.StoragesRepository
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class StoragesRepositoryImpl: StoragesRepository {
    companion object {
        private val imageFormats = listOf("png", "jpg", "jpeg")
        private val videoFormats = listOf("mp4", "avi")
    }

    // Получение списка имеющихся в общем хранилище медиафайлов
    override fun getAllMediaFiles(type: MediaType): List<MediaItemDto> {
        val context = App.appContext
        val items = mutableListOf<MediaItemDto>()

        when(type){
            MediaType.PHOTO -> { // Получение фото файлов
                getMediaFiles(
                    context,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    items,
                    imageFormats
                    , "I"
                )
            }
            MediaType.VIDEO -> { // Получение видео файлов
                getMediaFiles(
                    context,
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    items,
                    videoFormats,
                    "V"
                )
            }
            else -> { // Получение и фото и видео файлов
                getMediaFiles(context,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    items,
                    imageFormats,
                    "I"
                )
                getMediaFiles(
                    context,
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    items,
                    videoFormats,
                    "V"
                )
            }
        }
        // Сортировка по дате создания (новые сверху)
        return items.sortedByDescending { it.creationTime }
    }

    private fun getMediaFiles(
        context: Context,
        contentUri: Uri,
        items: MutableList<MediaItemDto>,
        allowedFormats: List<String>,
        mediaFileType: String
    ) {
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DATE_TAKEN,
            MediaStore.MediaColumns.DATA
        )

        val cursor: Cursor? = context.contentResolver.query(
            contentUri,
            projection,
            null, // Без дополнительных условий фильтрации
            null,
            "${MediaStore.MediaColumns.DATE_TAKEN} DESC" // Сортировка по дате (новые сверху)
        )

        cursor?.use { c ->
            val idIndex = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val dateIndex = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_TAKEN)
            val dataIndex = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)

            while (c.moveToNext()) {
                val id = c.getLong(idIndex)
                val uri = Uri.withAppendedPath(contentUri, id.toString())
                val creationTime = c.getLong(dateIndex)
                val filePath = c.getString(dataIndex)
                val mediaFileFormat = filePath.substringAfterLast('.', "").lowercase()

                // Скип пустых или поврежденных (если creationTime <= 0) и неподходящих форматов
                if (mediaFileFormat in allowedFormats && creationTime > 0) {
                    items.add(MediaItemDto(uri, creationTime, mediaFileFormat, mediaFileType))
                }
            }
        }
    }

    // Сохранение медиафайла из общего хранилища во внутреннем
    override fun saveImageToPrivateStorage(sourceUri: Uri): String? {
        val context = App.appContext
        // Получение оригинального имени файла и извлечение расширения
        val originalName = getFileName(context, sourceUri)
        val extension = originalName?.substringAfterLast('.', "")?.takeIf { it.isNotEmpty() } ?: ""

        // Генерация имени файла на основе текущей даты и времени, с добавлением расширения
        val dateTime = System.currentTimeMillis().toString()
        val fileName = if (extension.isNotEmpty()) {
            "mediafile_$dateTime.$extension"
        } else {
            "mediafile_$dateTime"
        }

        val outputFile = File(context.filesDir, fileName)

        // Получение размера исходного файла
        val fileSize = getFileSize(context, sourceUri)
        // Проверка доступного места во внутреннем хранилище
        val stat = StatFs(context.filesDir.absolutePath)
        val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
        if (fileSize > availableBytes) {
            Toast.makeText(context, context.resources.getString(R.string.out_of_memory), Toast.LENGTH_SHORT).show()
            return null
        }

        // Копирование файла с обработкой исключений
        return try {
            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                FileOutputStream(outputFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
                fileName  // Возврат имени файла при успехе
            }
        } catch (_: IOException) {
            // Обработка ошибок ввода-вывода
            Toast.makeText(context, context.resources.getString(R.string.copy_error), Toast.LENGTH_SHORT).show()
            null  // Возврат null при ошибке
        } catch (_: Exception) {
            // Обработка других исключений (например, SecurityException)
            Toast.makeText(context, context.resources.getString(R.string.unexpected_error), Toast.LENGTH_SHORT).show()
            null  // Возврат null
        }
    }

    // Получение размера файла из Uri
    private fun getFileSize(context: Context, uri: Uri): Long {
        val cursor = context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)
        return cursor?.use {
            if (it.moveToFirst()) {
                it.getLong(it.getColumnIndexOrThrow(OpenableColumns.SIZE))
            } else {
                0L
            }
        } ?: 0L
    }

    // Получение имени файла из Uri
    private fun getFileName(context: Context, uri: Uri): String? {
        val cursor = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        return cursor?.use {
            if (it.moveToFirst()) {
                it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
            } else {
                null
            }
        }
    }
    // Получение URI файла во внутреннем хранилище
    override fun getFileUri(fileName: String): Uri? {
        val context = App.appContext
        val file = File(context.filesDir, fileName)
        return if (file.exists()) {
            file.toUri()
        } else {
            null  // Файл не найден
        }
    }

}