package com.a2t.myapplication.main.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.a2t.myapplication.App
import com.a2t.myapplication.main.domain.api.MainInteractor
import com.a2t.myapplication.main.domain.model.ListRecord
import com.a2t.myapplication.main.presentation.model.SpecialMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainViewModel (
    private val mainInteractor: MainInteractor
) : ViewModel() {

    // Добавление новой записи и получение ее id
    suspend fun insertRecord(record: ListRecord)= withContext(Dispatchers.IO) {
        mainInteractor.insertRecord(record)
    }
    // Обновление записи
    fun updateRecord(record: ListRecord) {
        mainInteractor.updateRecord(record)
    }

    suspend fun getRecords(specialMode: SpecialMode, idDir: Long): List<ListRecord> {
        val records = when(specialMode) {
            SpecialMode.NORMAL, SpecialMode.MOVE, SpecialMode.DELETE -> {
                if (App.appSettings.sortingChecks) {
                    getRecordsForNormalMoveDeleteModesByCheck(idDir)
                } else {
                    getRecordsForNormalMoveDeleteModes(idDir)
                }
            }
            SpecialMode.RESTORE -> {
                if (App.appSettings.sortingChecks) {
                    getRecordsForRestoreArchiveModesByCheck(idDir, 1)
                } else {
                    getRecordsForRestoreArchiveModes(idDir, 1)
                }
            }
            SpecialMode.ARCHIVE -> {
                if (App.appSettings.sortingChecks) {
                    getRecordsForRestoreArchiveModesByCheck(idDir, 0)
                } else {
                    getRecordsForRestoreArchiveModes(idDir, 0)
                }
            }
        }
        val mutableRecords = records.toMutableList()
        if (specialMode == SpecialMode.NORMAL) {
            mutableRecords.add(getNewRecord(idDir, mutableRecords,mutableRecords.isEmpty() && App.appSettings.editEmptyDir))
        }
        return mutableRecords
    }

    private fun getNewRecord (idDir: Long, records: List<ListRecord>, startEdit: Boolean): ListRecord {
        return ListRecord(
            0,
            idDir,
            false,
            getMaxNpp(records) + 1,
            false,
            "",
            "",
            0,
            0,
            0,
            0,
            null,
            null,
            isArchive = false,
            isDelete = false,
            isFull = false,
            isAllCheck = false,
            true,
            startEdit,
            false
        )
    }

    private fun getMaxNpp (records: List<ListRecord>): Int {
        var maxNpp = 0
        for (rec: ListRecord in records) {
            if (rec.npp > maxNpp) maxNpp = rec.npp
        }
        return maxNpp
    }

    // Возвращает список записей для режимов NORMAL, MOVE, DELETE
    private suspend fun getRecordsForNormalMoveDeleteModes(idDir: Long)= withContext(Dispatchers.IO) {
        mainInteractor.getRecordsForNormalMoveDeleteModes(idDir)
    }
    // Возвращает список записей для режимов NORMAL, MOVE, DELETE с сортировкой по isChecked
    private suspend fun getRecordsForNormalMoveDeleteModesByCheck(idDir: Long)= withContext(Dispatchers.IO) {
        mainInteractor.getRecordsForNormalMoveDeleteModesByCheck(idDir)
    }
    // Возвращает список записей для режимов RESTORE(isDelete = 1), ARCHIVE(isDelete = 0)
    private suspend fun getRecordsForRestoreArchiveModes(idDir: Long, isDelete: Int)= withContext(Dispatchers.IO) {
        mainInteractor.getRecordsForRestoreArchiveModes(idDir, isDelete)
    }
    // Возвращает список записей для режимов RESTORE(isDelete = 1), ARCHIVE(isDelete = 0) с сортировкой по isChecked
    private suspend fun getRecordsForRestoreArchiveModesByCheck(idDir: Long, isDelete: Int)= withContext(Dispatchers.IO) {
        mainInteractor.getRecordsForRestoreArchiveModesByCheck(idDir, isDelete)
    }

    // Возвращает список имен папок с одним элементом - именем папки с id = idDir
    fun getNameDir (idDir: Long): LiveData<List<String>> {
        return mainInteractor.getNameDir(idDir)
    }
    // Возвращает список id родительских папок с одним элементом - id родительской папки для папки с id = idDir
    fun getParentDir(idDir: Long): LiveData<List<Long>> {
        return mainInteractor.getParentDir(idDir)
    }
}