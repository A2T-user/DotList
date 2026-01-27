package com.a2t.myapplication.main.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a2t.myapplication.common.App
import com.a2t.myapplication.main.domain.api.GCInteractor
import com.a2t.myapplication.main.ui.activity.model.SpecialMode
import com.a2t.myapplication.main.domain.api.MainInteractor
import com.a2t.myapplication.main.domain.model.ListRecord
import com.a2t.myapplication.main.ui.fragments.models.TextFragmentMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(
    private val mainInteractor: MainInteractor,
    private val gcInteractor: GCInteractor
): ViewModel() {
    // Параметры для MainActivity
    val moveBuffer = ArrayList<ListRecord>()    // Буфер для режима MOVE(только перенос записей)
    val mainBuffer = ArrayList<ListRecord>()    // Буфер для всех остальных специальных режимов
    var idDir = 0L
    var specialMode = SpecialMode.NORMAL

    // Параметры для TextFragment
    var textFragmentMode: TextFragmentMode? = null
    var idCurrentDir: Long = 0
    val mainRecords = ArrayList<ListRecord>()

    // Параметры для AlarmFragment
    var record: ListRecord? = null

    // Добавление новой записи и получение ее id
    fun insertRecord(record: ListRecord, callback: (Long) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val id = mainInteractor.insertRecord(record)
            withContext(Dispatchers.Main) {
                callback(id)
            }
        }
    }
    // Добавление новых записей
    fun insertRecords(records: List<ListRecord>) {
        mainInteractor.insertRecords(records)
    }

    // Обновление записи
    fun updateRecord(record: ListRecord, callback: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            mainInteractor.updateRecord(record)
            withContext(Dispatchers.Main) {
                callback()
            }
        }
    }
    // Обновление записей
    fun updateRecords(records: List<ListRecord>, callback: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            mainInteractor.updateRecords(records)
            withContext(Dispatchers.Main) {
                callback()
            }
        }
    }

    // Обновление записей при вставке
    fun pasteRecords(moveBuffer: List<ListRecord>, mainBuffer: List<ListRecord>, callback: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            mainInteractor.updateRecords(moveBuffer)
            copyRecords(mainBuffer, idDir)
            withContext(Dispatchers.Main) {
                callback()
            }
        }
    }

    fun getRecords(callback: (List<ListRecord>) -> Unit) {
        viewModelScope.launch {
            var records: List<ListRecord>
            when(specialMode) {
                SpecialMode.NORMAL, SpecialMode.MOVE, SpecialMode.DELETE -> {
                    records = if (App.appSettings.sortingChecks) {
                        getRecordsForNormalMoveDeleteModesByCheck()
                    } else {
                        getRecordsForNormalMoveDeleteModes()
                    }
                }

                SpecialMode.RESTORE -> {
                    records = if (App.appSettings.sortingChecks) {
                        getRecordsForRestoreArchiveModesByCheck(1)
                    } else {
                        getRecordsForRestoreArchiveModes(1)
                    }
                }

                SpecialMode.ARCHIVE -> {
                    records = if (App.appSettings.sortingChecks) {
                        getRecordsForRestoreArchiveModesByCheck(0)
                    } else {
                        getRecordsForRestoreArchiveModes(0)
                    }
                }
            }
            val mutableRecords = records.toMutableList()
            if (specialMode == SpecialMode.NORMAL) {
                mutableRecords.add(
                    getNewRecord(
                        mutableRecords,
                        mutableRecords.isEmpty() && App.appSettings.editEmptyDir
                    )
                )
            }
            callback(mutableRecords)
        }
    }

    private fun getNewRecord(records: List<ListRecord>, startEdit: Boolean): ListRecord {
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
            null,
            isArchive = false,
            isDelete = false,
            null,
            isFull = false,
            isAllCheck = false,
            true,
            startEdit,
            false
        )
    }

    private fun getMaxNpp(records: List<ListRecord>): Int {
        var maxNpp = 0
        for(rec: ListRecord in records) {
            if (rec.npp > maxNpp) maxNpp = rec.npp
        }
        return maxNpp
    }

    // Возвращает список записей для режимов NORMAL, MOVE, DELETE
    private suspend fun getRecordsForNormalMoveDeleteModes() = withContext(Dispatchers.IO) {
        mainInteractor.getRecordsForNormalMoveDeleteModes(idDir)
    }
    // Возвращает список записей для режимов NORMAL, MOVE, DELETE с сортировкой по isChecked
    private suspend fun getRecordsForNormalMoveDeleteModesByCheck() = withContext(Dispatchers.IO) {
        mainInteractor.getRecordsForNormalMoveDeleteModesByCheck(idDir)
    }
    // Возвращает список записей для режимов RESTORE(isDelete = 1), ARCHIVE(isDelete = 0)
    private suspend fun getRecordsForRestoreArchiveModes(isDelete: Int) = withContext(Dispatchers.IO) {
        mainInteractor.getRecordsForRestoreArchiveModes(idDir, isDelete)
    }
    // Возвращает список записей для режимов RESTORE(isDelete = 1), ARCHIVE(isDelete = 0) с сортировкой по isChecked
    private suspend fun getRecordsForRestoreArchiveModesByCheck(isDelete: Int) = withContext(
        Dispatchers.IO) {
        mainInteractor.getRecordsForRestoreArchiveModesByCheck(idDir, isDelete)
    }

    // Возвращает список имен папок с одним элементом - именем папки с id = idDir
    fun getNameDir(id: Long, callback: (List<String>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val names = mainInteractor.getNameDir(id)
            withContext(Dispatchers.Main) {
                callback(names)
            }
        }
    }
    // Возвращает список id родительских папок с одним элементом - id родительской папки для папки с id = idDir
    fun getParentDirId(id: Long, callback: (List<Long>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val ids = mainInteractor.getParentDirId(id)
            withContext(Dispatchers.Main) {
                callback(ids)
            }
        }
    }
    // Возвращает список id родительских папок для определения рекурсии
    fun checkingRecursion(idCurrentDir: Long, pasteIds: List<Long>, callbackMain: () -> Unit, callbackIo: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val parentDirIds = mutableListOf(idCurrentDir)
            selectionParentDirIdForRecursion(idCurrentDir, parentDirIds)
            if (pasteIds.any { it in parentDirIds }) {
                withContext(Dispatchers.Main) {
                    callbackMain()
                }
            } else {
                callbackIo()
            }
        }
    }
    private fun selectionParentDirIdForRecursion(idDir: Long, parentDirIds: MutableList<Long>) {
        val parentIds = mainInteractor.getParentDirId(idDir)
        if (parentIds.isNotEmpty()) {
            val parentId = parentIds[0]
            parentDirIds.add(parentId)
            selectionParentDirIdForRecursion(parentId, parentDirIds)
        }
    }

    // Возвращает список подчиненных записей для удаления
    fun selectionSubordinateRecordsToDelete(records: List<ListRecord>, callback: (List<ListRecord>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val mutableRecords = mutableListOf<ListRecord>()
            getSubordinateRecordsToDelete(records, mutableRecords)
            withContext(Dispatchers.Main) {
                callback(mutableRecords)
            }
        }
    }
    private fun getSubordinateRecordsToDelete(records: List<ListRecord>, mutableRecords: MutableList<ListRecord>) {
        records.forEach { record ->
            if (record.isDir) {
                val selectionRecords = mainInteractor.selectionSubordinateRecordsToDelete(record.id)
                mutableRecords.addAll(selectionRecords)
                getSubordinateRecordsToDelete(selectionRecords, mutableRecords)
            }
        }
    }
    // Возвращает список подчиненных записей для восстановления
    fun selectionSubordinateRecordsToRestore(records: List<ListRecord>, callback: (List<ListRecord>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val mutableRecords = mutableListOf<ListRecord>()
            getSubordinateRecordsToRestore(records, mutableRecords)
            withContext(Dispatchers.Main) {
                callback(mutableRecords)
            }
        }
    }
    private fun getSubordinateRecordsToRestore(records: List<ListRecord>, mutableRecords: MutableList<ListRecord>) {
        records.forEach { record ->
            if (record.isDir) {
                val selectionRecords = mainInteractor.selectionSubordinateRecordsToRestore(record.id)
                mutableRecords.addAll(selectionRecords)
                getSubordinateRecordsToRestore(selectionRecords, mutableRecords)
            }
        }
    }

    private fun copyRecords(records: List<ListRecord>, idDir: Long) {
        records.forEach { record ->
            val cloneRecord = record.copy(id = 0, idDir = idDir)
            val newId = mainInteractor.insertRecord(cloneRecord)
            if (record.isDir) {
                val selectionRecords = mainInteractor.selectionSubordinateRecordsToDelete(record.id)
                copyRecords(selectionRecords, newId)
            }
        }
    }

    // Удаление записей с итекшим сроком хранения
    fun deletingExpiredRecords(callback: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            mainInteractor.deletingExpiredRecords()
            withContext(Dispatchers.Main) {
                callback()
            }
        }
    }

    // Обнуляем устаревшие Alarm-ы
    fun deleteOldAlarm(time: Long, callback: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            mainInteractor.deleteOldAlarm(time)
            withContext(Dispatchers.Main) {
                callback()
            }
        }
    }

    // Запуск GarbageCollector для удаления неиспользуемых файлов
    fun startGarbageCollector() {
        viewModelScope.launch {
            gcInteractor.run()
        }
    }
}