package com.a2t.myapplication.main.presentation


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a2t.myapplication.App
import com.a2t.myapplication.main.domain.api.MainInteractor
import com.a2t.myapplication.main.domain.model.ListRecord
import com.a2t.myapplication.main.presentation.model.SpecialMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel (
    private val mainInteractor: MainInteractor
) : ViewModel() {
    val moveBuffer = ArrayList<ListRecord>()    // Буфер для режима MOVE(только перенос записей)
    val mainBuffer = ArrayList<ListRecord>()    // Буфер для всех остальных специальных режимов

    // Обновление записи
    fun updateRecord(record: ListRecord) {
        mainInteractor.updateRecord(record)
    }
    // Обновление записей
    fun updateRecords(records: List<ListRecord>) {
        mainInteractor.updateRecords(records)
    }
    // Добавление новой записи и получение ее id
    fun insertRecord(record: ListRecord, callback: (Long) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val id = mainInteractor.insertRecord(record)
            callback(id)
        }

    }

    suspend fun getRecords(specialMode: SpecialMode, idDir: Long, callback: (List<ListRecord>) -> Unit) {
        viewModelScope.launch {
            var records = listOf<ListRecord>()
            when (specialMode) {
                SpecialMode.NORMAL, SpecialMode.MOVE, SpecialMode.DELETE -> {
                    records = if (App.appSettings.sortingChecks) {
                        getRecordsForNormalMoveDeleteModesByCheck(idDir)
                    } else {
                        getRecordsForNormalMoveDeleteModes(idDir)
                    }
                }

                SpecialMode.RESTORE -> {
                    records = if (App.appSettings.sortingChecks) {
                        getRecordsForRestoreArchiveModesByCheck(idDir, 1)
                    } else {
                        getRecordsForRestoreArchiveModes(idDir, 1)
                    }
                }

                SpecialMode.ARCHIVE -> {
                    records = if (App.appSettings.sortingChecks) {
                        getRecordsForRestoreArchiveModesByCheck(idDir, 0)
                    } else {
                        getRecordsForRestoreArchiveModes(idDir, 0)
                    }
                }
            }
            val mutableRecords = records.toMutableList()
            if (specialMode == SpecialMode.NORMAL) {
                mutableRecords.add(
                    getNewRecord(
                        idDir,
                        mutableRecords,
                        mutableRecords.isEmpty() && App.appSettings.editEmptyDir
                    )
                )
            }
            callback(mutableRecords)
        }
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
    private suspend fun getRecordsForNormalMoveDeleteModes(idDir: Long) = withContext(Dispatchers.IO) {
        mainInteractor.getRecordsForNormalMoveDeleteModes(idDir)
    }
    // Возвращает список записей для режимов NORMAL, MOVE, DELETE с сортировкой по isChecked
    private suspend fun getRecordsForNormalMoveDeleteModesByCheck(idDir: Long) = withContext(Dispatchers.IO) {
        mainInteractor.getRecordsForNormalMoveDeleteModesByCheck(idDir)
    }
    // Возвращает список записей для режимов RESTORE(isDelete = 1), ARCHIVE(isDelete = 0)
    private suspend fun getRecordsForRestoreArchiveModes(idDir: Long, isDelete: Int) = withContext(Dispatchers.IO) {
        mainInteractor.getRecordsForRestoreArchiveModes(idDir, isDelete)
    }
    // Возвращает список записей для режимов RESTORE(isDelete = 1), ARCHIVE(isDelete = 0) с сортировкой по isChecked
    private suspend fun getRecordsForRestoreArchiveModesByCheck(idDir: Long, isDelete: Int) = withContext(Dispatchers.IO) {
        mainInteractor.getRecordsForRestoreArchiveModesByCheck(idDir, isDelete)
    }

    // Возвращает список имен папок с одним элементом - именем папки с id = idDir
    fun getNameDir (idDir: Long, callback: (List<String>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val names = mainInteractor.getNameDir(idDir)
            callback(names)
        }
    }
    // Возвращает список id родительских папок с одним элементом - id родительской папки для папки с id = idDir
    fun getParentDir(idDir: Long, callback: (List<Long>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val ids = mainInteractor.getParentDir(idDir)
            callback(ids)
        }
    }

    // Возвращает список подчиненных записей для удаления
    fun selectionSubordinateRecordsToDelete(records: List<ListRecord>, callback: (List<ListRecord>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val mutableRecords = mutableListOf<ListRecord>()
            getSubordinateRecordsToDelet(records, mutableRecords)
            withContext(Dispatchers.Main) {
                callback(mutableRecords)
            }
        }
    }

    private fun getSubordinateRecordsToDelet(records: List<ListRecord>, mutableRecords: MutableList<ListRecord>) {
        records.forEach { record ->
            if (record.isDir) {
                val selectionRecords = mainInteractor.selectionSubordinateRecordsToDelete(record.id)
                mutableRecords.addAll(selectionRecords)
                getSubordinateRecordsToDelet(selectionRecords, mutableRecords)
            }
        }
    }

    // Удаление записей с итекшим сроком хранения
    fun deletingExpiredRecords() {
        mainInteractor.deletingExpiredRecords()
    }
}