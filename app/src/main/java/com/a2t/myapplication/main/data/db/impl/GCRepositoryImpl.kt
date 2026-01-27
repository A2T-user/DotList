package com.a2t.myapplication.main.data.db.impl

import android.util.Log
import com.a2t.myapplication.common.App
import com.a2t.myapplication.common.data.AppDatabase
import com.a2t.myapplication.main.domain.api.GCRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class GCRepositoryImpl(
    private val appDatabase: AppDatabase
): GCRepository {

    override suspend fun run() {
        try {   // Выполняем очистку файлов
            withContext(Dispatchers.IO) {
                cleanupOrphanedFiles()
            }
        } catch (e: Exception) {
            Log.e("Error", "Garbage Collector Error: ${e.message}")
        }
    }

    private suspend fun cleanupOrphanedFiles() {
        try {
            // 1. Получаем список файлов из указанных папок
            val storageFiles = getFilesFromStorage()
            if (storageFiles.isEmpty()) return
            // 2. Получаем сгруппированный список имен файлов из БД
            val dbFileNames = appDatabase.listItemDao().getDistinctMediaFiles().toSet()
            // 3. Проверяем и удаляем файлы, которых нет в БД
            deleteOrphanedFiles(storageFiles, dbFileNames)
        } catch (e: Exception) {
            throw e
        }
    }

    private data class StorageFile(
        val file: File,
        val fileName: String
    )

    private fun getFilesFromStorage(): List<StorageFile> {
        val storageFiles = mutableListOf<StorageFile>()
        val baseDir = App.appContext.getExternalFilesDir(null)
        listOf(
            File(baseDir, "mediafiles/image"),
            //File(baseDir, "mediafiles/video")
        ).forEach { folder ->
            if (folder.exists() && folder.isDirectory) {
                folder.listFiles()?.forEach { file ->
                    if (file.isFile) {
                        storageFiles.add(
                            StorageFile(
                                file = file,
                                fileName = file.name
                            )
                        )
                    }
                }
            }
        }
        return storageFiles
    }

    private fun deleteOrphanedFiles(
        storageFiles: List<StorageFile>,
        dbFileNames: Set<String>
    ) {
        storageFiles.forEach { storageFile ->
            if (!dbFileNames.contains(storageFile.fileName)) {
                if (storageFile.file.exists()) storageFile.file.delete()
            }
        }
    }
}