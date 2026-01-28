package com.a2t.myapplication.mediafile.data.impl

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.core.net.toUri
import com.a2t.myapplication.common.App
import com.a2t.myapplication.common.utilities.AppHelper
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
        // Получение файлов из внутреннего хранилища
        getExternalAppMediaFiles(
            context,
            items,
            MediaFileType.IMAGE
        )
        return items.sortedByDescending { it.creationTime } // Сортировка по дате (новые сверху)
    }
    // Получение файлов из внешнего хранилища приложения
    private fun getExternalAppMediaFiles(
        context: Context,
        items: MutableList<MediaItemDto>,
        mediaFileType: MediaFileType
    ) {
        val externalFilesDir = context.getExternalFilesDir(null) ?: return
        val subDirName = when (mediaFileType) {
            MediaFileType.IMAGE -> "image"
            MediaFileType.VIDEO -> "video"
        }
        val typeDir = File(externalFilesDir, "mediafiles/$subDirName")
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

    // Сохранение медиафайла из общего хранилища во внешнем хранилище приложения
    override fun saveFileToExternalAppStorage(
        sourceUri: Uri,
        mediaFileType: MediaFileType,
        dir: DirType,
        isNewCopy: Boolean
    ): Response {
        val context = App.appContext
        val originalName = AppHelper.getFileNameFromUri(sourceUri) ?: return Response.Error(ErrCode.UNEXPECTED_ERROR)
        if (dir == DirType.APP) {
            return Response.FileFromExternalAppStorage(originalName)
        }
        val fileName = if (isNewCopy) {
            val timestamp = System.currentTimeMillis()
            "${timestamp}#$originalName"
        } else {
            originalName
        }
        val subDirName = when (mediaFileType) {
            MediaFileType.IMAGE -> "image"
            MediaFileType.VIDEO -> "video"
        }
        val mediaFilesDir = File(context.getExternalFilesDir(null), "mediafiles")
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
        // Проверка доступного места во внешнем хранилище приложения
        val externalDir = context.getExternalFilesDir(null)
        val statFs = StatFs(externalDir!!.absolutePath)
        val availableSpace = statFs.availableBytes
        if (fileSize > availableSpace) {
            return Response.Error(ErrCode.OUT_OF_MEMORY)
        }
        // Копирование файла с обработкой исключений
        return try {
            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                FileOutputStream(outputFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            Response.Success(fileName, outputFile.toUri(), mediaFileType)
        } catch (_: IOException) {
            Response.Error(ErrCode.COPY_ERROR)
        } catch (_: Exception) {
            Response.Error(ErrCode.UNEXPECTED_ERROR)
        }
    }

    // Получение размера файла из Uri
    private fun getFileSize(context: Context, uri: Uri): Long {
        return when {
            uri.scheme == "content" -> {
                val cursor = context.contentResolver.query(
                    uri,
                    arrayOf(OpenableColumns.SIZE),
                    null,
                    null,
                    null
                )
                cursor?.use {
                    if (it.moveToFirst()) {
                        it.getLong(it.getColumnIndexOrThrow(OpenableColumns.SIZE))
                    } else {
                        0L
                    }
                } ?: 0L
            }
            uri.scheme == "file" -> {
                File(uri.path!!).length()
            }
            else -> {
                0L
            }
        }
    }

    // Сохранение медиафайла из внешнего хранилища приложения в общем хранилище
    override suspend fun copyFileFromExternalAppToPublicStorage(
        sourceUri: Uri,
        mediaFileType: MediaFileType
    ): Response {
        val context = App.appContext
        // Получаем имя файла из URI
        var fileName = AppHelper.getFileNameFromUri(sourceUri) ?: return Response.Error(ErrCode.UNEXPECTED_ERROR)
        fileName = fileName.substringAfterLast("#")     // Отбрасываем префикс
        val fileSize = getFileSize(context, sourceUri)  // Получение размера исходного файла
        // Проверка доступного места во общем хранилище
        val stat = StatFs(context.getExternalFilesDir(null)?.getParentFile()?.path)
        val availableBytes = stat.availableBytes
        if (fileSize > availableBytes) {
            return Response.Error(ErrCode.OUT_OF_MEMORY)
        }
        // Определяем URI назначения (общее хранилище)
        val destinationUri = when (mediaFileType) {
            MediaFileType.IMAGE -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            MediaFileType.VIDEO -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }
        val mimeType = AppHelper.getMimeType(fileName)
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, when (mediaFileType) {
                    MediaFileType.IMAGE -> Environment.DIRECTORY_PICTURES
                    MediaFileType.VIDEO -> Environment.DIRECTORY_MOVIES
                })
            }
        }
        val resolver = context.contentResolver
        val newUri = resolver.insert(destinationUri, contentValues)
        if (newUri != null) {
            var success = false
            return try {
                resolver.openInputStream(sourceUri)?.use { inputStream ->
                    resolver.openOutputStream(newUri)?.use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                success = true
                Response.Success(fileName, newUri, mediaFileType)
            } catch (_: IOException) {
                Response.Error(ErrCode.COPY_ERROR)
            } catch (_: Exception) {
                Response.Error(ErrCode.UNEXPECTED_ERROR)
            } finally {
                // Если произошла ошибка, удаляем частично созданный файл
                if (!success) {
                    try {
                        resolver.delete(newUri, null, null)
                    } catch (_: Exception) {} // Игнорируем ошибки удаления
                }
            }
        } else {
            return Response.Error(ErrCode.UNEXPECTED_ERROR)
        }
    }
}