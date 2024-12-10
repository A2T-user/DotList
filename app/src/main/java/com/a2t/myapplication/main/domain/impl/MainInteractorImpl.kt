package com.a2t.myapplication.main.domain.impl

import com.a2t.myapplication.main.domain.api.MainInteractor
import com.a2t.myapplication.main.domain.api.MainRepository
import com.a2t.myapplication.main.domain.model.ListRecord

class MainInteractorImpl(
    private val mainRepository: MainRepository
): MainInteractor {
    // Добавление новой записи
    override fun insertRecord(record: ListRecord): Long {
        return mainRepository.insertRecord(record)
    }
    // Добавление новых записей
    override fun insertRecords(records: List<ListRecord>) {
        mainRepository.insertRecords(records)
    }

    // Обновление записи
    override fun updateRecord(record: ListRecord) {
        mainRepository.updateRecord(record)
    }
    // Обновление записей
    override fun updateRecords(records: List<ListRecord>) {
        mainRepository.updateRecords(records)
    }

    // Возвращает список записей для режимов NORMAL, MOVE, DELETE
    override fun getRecordsForNormalMoveDeleteModes(idDir: Long): List<ListRecord> {
        return mainRepository.getRecordsForNormalMoveDeleteModes(idDir)
    }
    // Возвращает список записей для режимов NORMAL, MOVE, DELETE с сортировкой по isChecked
    override fun getRecordsForNormalMoveDeleteModesByCheck(idDir: Long): List<ListRecord> {
        return mainRepository.getRecordsForNormalMoveDeleteModesByCheck(idDir)
    }
    // Возвращает список записей для режимов RESTORE(isDelete = 1), ARCHIVE(isDelete = 0)
    override fun getRecordsForRestoreArchiveModes(idDir: Long, isDelete: Int): List<ListRecord> {
        return mainRepository.getRecordsForRestoreArchiveModes(idDir, isDelete)
    }
    // Возвращает список записей для режимов RESTORE(isDelete = 1), ARCHIVE(isDelete = 0) с сортировкой по isChecked
    override fun getRecordsForRestoreArchiveModesByCheck(idDir: Long, isDelete: Int): List<ListRecord> {
        return mainRepository.getRecordsForRestoreArchiveModesByCheck(idDir, isDelete)
    }


    // Возвращает список имен папок с одним элементом - именем папки с id = idDir
    override fun getNameDir(idDir: Long): List<String> {
        return mainRepository.getNameDir(idDir)
    }
    // Возвращает список id родительских папок с одним элементом - id родительской папки для папки с id = idDir
    override fun getParentDirId(idDir: Long): List<Long> {
        return mainRepository.getParentDirId(idDir)
    }

    // Возвращает список подчиненных записей для удаления
    override fun selectionSubordinateRecordsToDelete(idDir: Long): List<ListRecord> {
        return mainRepository.selectionSubordinateRecordsToDelete(idDir)
    }
    // Возвращает список подчиненных записей для восстановления
    override fun selectionSubordinateRecordsToRestore(idDir: Long): List<ListRecord> {
        return mainRepository.selectionSubordinateRecordsToRestore(idDir)
    }

    // Удаление записей с итекшим сроком хранения
    override fun deletingExpiredRecords() {
        mainRepository.deletingExpiredRecords()
    }
}