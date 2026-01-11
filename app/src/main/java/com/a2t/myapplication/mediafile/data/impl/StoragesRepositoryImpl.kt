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
import com.a2t.myapplication.mediafile.data.dto.DirType
import com.a2t.myapplication.mediafile.data.dto.ErrCode
import com.a2t.myapplication.mediafile.data.dto.MediaFileType
import com.a2t.myapplication.mediafile.data.dto.MediaItemDto
import com.a2t.myapplication.mediafile.data.dto.Response
import com.a2t.myapplication.mediafile.domaim.api.StoragesRepository
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class StoragesRepositoryImpl(
    private val contentResolver: ContentResolver
): StoragesRepository {

    // Получение списка имеющихся в общем хранилище и в загрузках медиафайлов
    override fun getAllMediaFiles(): List<MediaItemDto> {
        val context = App.appContext
        val items = mutableListOf<MediaItemDto>()
        // Получение файлов из общего хранилища
        getMediaFiles(context,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,   // Картинки
            items,
            MediaFileType.IMAGE
        )
        getMediaFiles(
            context,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,    // Видео
            items,
            MediaFileType.VIDEO
        )
        // Получение файлов из внутреннего хранилища
        getInternalMediaFiles(
            context,
            items,
            MediaFileType.IMAGE
        )
        getInternalMediaFiles(
            context,
            items,
            MediaFileType.VIDEO
        )
        return items.sortedByDescending { it.creationTime } // Сортировка по дате (новые сверху)
    }
    // Получение файлов из внутреннего хранилища
    private fun getInternalMediaFiles(
        context: Context,
        items: MutableList<MediaItemDto>,
        mediaFileType: MediaFileType
    ) {
        val subDirName = when (mediaFileType) {
            MediaFileType.IMAGE -> "image"
            MediaFileType.VIDEO -> "video"
        }
        val typeDir = File(context.filesDir, "mediafiles/$subDirName")
        // Проверяем, существует ли папка
        if (!typeDir.exists() || !typeDir.isDirectory) return

        // Вспомогательная функция для обхода папок
        fun scanDirectory(currentDir: File) {
            val files = currentDir.listFiles()
            files?.forEach { file ->
                if (file.isDirectory) {
                    scanDirectory(file)
                } else if (file.isFile) {
                    parseFile(file, mediaFileType).let { items.add(it) }
                }
            }
        }

        scanDirectory(typeDir)        // Запускаем сканирование от папки mediafiles
    }
    private fun parseFile(file: File, mediaFileType: MediaFileType): MediaItemDto = MediaItemDto(
        uri = Uri.fromFile(file),           // Превращаем путь к файлу в Uri
        creationTime = file.lastModified(), // Берем время последнего изменения
        mediaFileType = mediaFileType,
        DirType.APP
    )

    // Получение медиа файлов из галереи
    private fun getMediaFiles(
        context: Context,
        contentUri: Uri,
        items: MutableList<MediaItemDto>,
        mediaFileType: MediaFileType
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
            null,
            null,
            null
        )
        cursor?.use { c ->
            val idIndex = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val dateTakenIndex = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_TAKEN)
            val dateModifiedIndex = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
            val dateAddedIndex = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)

            while (c.moveToNext()) {
                val id = c.getLong(idIndex)
                val uri = Uri.withAppendedPath(contentUri, id.toString())
                val creationTime = c.getLong(dateTakenIndex)
                val modifiedTime = c.getLong(dateModifiedIndex)
                val addedTime = c.getLong(dateAddedIndex)*1000
                val maxTime = maxOf(creationTime, modifiedTime, addedTime)
                items.add(MediaItemDto(uri, maxTime,  mediaFileType, DirType.GALLERY))
            }
        }
        cursor?.close()
    }

    // Сохранение медиафайла из общего хранилища во внутреннем
    override fun saveImageToPrivateStorage(
        sourceUri: Uri,
        mediaFileType: MediaFileType,
        deleteSourceAfterSave: Boolean,
        isNewCopy: Boolean
    ): Response {
        val context = App.appContext
        val originalName = getFileName(context, sourceUri) ?: return Response.Error(ErrCode.UNEXPECTED_ERROR)

        val fileName = if (isNewCopy) {
            val timestamp = System.currentTimeMillis()
            "${timestamp}_$originalName"
        } else {
            originalName
        }
        val subDirName = when (mediaFileType) {
            MediaFileType.IMAGE -> "image"
            MediaFileType.VIDEO -> "video"
        }
        val mediaFilesDir = File(context.filesDir, "mediafiles")
        val typeSpecificDir = File(mediaFilesDir, subDirName)

        if (!typeSpecificDir.exists()) {
            typeSpecificDir.mkdirs() // mkdirs создает все промежуточные директории
        }
        val outputFile = File(typeSpecificDir, fileName)
        // Проверка, существует ли файл с таким именем
        if (outputFile.exists()) {
            return  Response.FileExists(originalName, sourceUri, mediaFileType) // Вернуть имя файла, если файл уже существует
        }
        // Получение размера исходного файла
        val fileSize = getFileSize(context, sourceUri)
        // Проверка доступного места во внутреннем хранилище
        val stat = StatFs(context.filesDir.absolutePath)
        val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
        if (fileSize > availableBytes) {
            return Response.Error(ErrCode.OUT_OF_MEMORY)
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
                } catch (e: Exception) {
                    Log.e("saveImageToPrivateStorage", "Ошибка удаления исходного файла: ${e.message}")
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
}