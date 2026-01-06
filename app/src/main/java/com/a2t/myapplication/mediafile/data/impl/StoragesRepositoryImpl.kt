package com.a2t.myapplication.mediafile.data.impl

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.net.toUri
import com.a2t.myapplication.common.App
import com.a2t.myapplication.mediafile.data.dto.ErrCode
import com.a2t.myapplication.mediafile.data.dto.MediaItemDto
import com.a2t.myapplication.mediafile.data.dto.Response
import com.a2t.myapplication.mediafile.data.model.MediaType
import com.a2t.myapplication.mediafile.domaim.api.StoragesRepository
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class StoragesRepositoryImpl(
    private val contentResolver: ContentResolver
): StoragesRepository {

    // Получение списка имеющихся в общем хранилище и в загрузках медиафайлов
    override fun getAllMediaFiles(type: MediaType): List<MediaItemDto> {
        val context = App.appContext
        val items = mutableListOf<MediaItemDto>()

        when(type){
            MediaType.PHOTO -> { // Получение фото файлов
                getMediaFiles(
                    context,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    items,
                    "I"
                )
            }
            MediaType.VIDEO -> { // Получение видео файлов
                getMediaFiles(
                    context,
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    items,
                    "V"
                )
            }
            else -> { // Получение и фото и видео файлов
                getMediaFiles(context,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    items,
                    "I"
                )
                getMediaFiles(
                    context,
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    items,
                    "V"
                )
            }
        }
        // Сортировка по дате создания (новые сверху)
        return items.sortedByDescending { it.creationTime }
    }
    // Получение медиа файлов из галереи
    private fun getMediaFiles(
        context: Context,
        contentUri: Uri,
        items: MutableList<MediaItemDto>,
        mediaFileType: String
    ) {
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DATE_TAKEN,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.DATE_ADDED,
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
            val dateTakenIndex = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_TAKEN)
            val dateModifiedIndex = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
            val dateAddedIndex = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            val dataIndex = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)

            while (c.moveToNext()) {
                val id = c.getLong(idIndex)
                val uri = Uri.withAppendedPath(contentUri, id.toString())
                val creationTime = c.getLong(dateTakenIndex)
                val modifiedTime = c.getLong(dateModifiedIndex)
                val addedTime = c.getLong(dateAddedIndex)*1000
                val maxTime = maxOf(creationTime, modifiedTime, addedTime)
                val filePath = c.getString(dataIndex)
                val mediaFileFormat = filePath.substringAfterLast('.', "").lowercase()
                items.add(MediaItemDto(uri, maxTime, mediaFileFormat, mediaFileType))
            }
        }
        cursor?.close()
    }

    // Сохранение медиафайла из общего хранилища во внутреннем
    override fun saveImageToPrivateStorage(
        sourceUri: Uri,
        mediaFileType: String,
        deleteSourceAfterSave: Boolean): Response {
        val context = App.appContext
        val originalName = getFileName(context, sourceUri) ?: return Response.Error(ErrCode.UNEXPECTED_ERROR)  // Если оригинальное имя не получено, вернуть null

        val prefix = when (mediaFileType) {
            "I" -> "image_"
            "V" -> "video_"
            else -> "mediafile_"
        }

        val fileName = "$prefix$originalName"
        val outputFile = File(context.filesDir, fileName)
        // Проверка, существует ли файл с таким именем
        if (outputFile.exists()) {
            return  Response.FileExists(fileName) // Вернуть имя файла, если файл уже существует
        }
        // Получение размера исходного файла
        val fileSize = getFileSize(context, sourceUri)
        // Проверка доступного места во внутреннем хранилище
        val stat = StatFs(context.filesDir.absolutePath)
        val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
        if (fileSize > availableBytes) {
            return Response.Error(ErrCode.OUT_OF_MEMORY) // Вернуть код ошибки, если файл неумещается
        }
        // Копирование файла с обработкой исключений
        return try {
            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                FileOutputStream(outputFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            if (deleteSourceAfterSave) {
                try {
                    context.contentResolver.delete(sourceUri, null, null)
                } catch (_: Exception) {
                    Log.e("saveImageToPrivateStorage", "Ошибка удаления исходного файла")
                }
            }
            Response.Success(fileName)
        } catch (_: IOException) {
            Response.Error(ErrCode.COPY_ERROR)
        } catch (_: Exception) {
            Response.Error(ErrCode.UNEXPECTED_ERROR)
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

    override suspend fun addPhotoToGallery(file: File): Uri? {
        return insertMediaFile(
            file = file,
            contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            mimeTypeStr = "image/jpeg",
            relativePath = Environment.DIRECTORY_PICTURES,
        )
    }

    override suspend fun addVideoToGallery(file: File): Uri? {
        return insertMediaFile(
            file = file,
            contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            mimeTypeStr = "video/mp4",
            relativePath = Environment.DIRECTORY_MOVIES,
        )
    }

    private fun insertMediaFile(
        file: File,
        contentUri: Uri,
        mimeTypeStr: String,
        relativePath: String
    ): Uri? {
        try {
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension.lowercase()) ?: mimeTypeStr
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            }
            // Вставка записи в MediaStore и получение URI
            val uri = contentResolver.insert(contentUri, contentValues) ?: return null  // Если вставка не удалась

            // Копирование файла в MediaStore
            try {
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    file.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                } ?: return null  // Если не удалось открыть поток
            } catch (_: Exception) {
                // Удалить частично созданную запись, если копирование провалилось
                contentResolver.delete(uri, null, null)
                return null
            }
            file.delete()   // Удаляем исходный файл
            return uri
        } catch (_: Exception) {
            return null
        }
    }
}