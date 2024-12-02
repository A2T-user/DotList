package com.a2t.myapplication.main.domain.api

import com.a2t.myapplication.main.domain.model.ListRecord


interface MainInteractor {
    // Добавление новой записи
    fun insertRecord(record: ListRecord): Long
    // Обновление записи
    fun updateRecord(record: ListRecord)
    // Обновление записей
    fun updateRecords(records: List<ListRecord>)

    // Возвращает список записей для режимов NORMAL, MOVE, DELETE
    fun getRecordsForNormalMoveDeleteModes(idDir: Long): List<ListRecord>
    // Возвращает список записей для режимов NORMAL, MOVE, DELETE с сортировкой по isChecked
    fun getRecordsForNormalMoveDeleteModesByCheck(idDir: Long): List<ListRecord>
    // Возвращает список записей для режимов RESTORE(isDelete = 1), ARCHIVE(isDelete = 0)
    fun getRecordsForRestoreArchiveModes(idDir: Long, isDelete: Int): List<ListRecord>
    // Возвращает список записей для режимов RESTORE(isDelete = 1), ARCHIVE(isDelete = 0) с сортировкой по isChecked
    fun getRecordsForRestoreArchiveModesByCheck(idDir: Long, isDelete: Int): List<ListRecord>

    // Возвращает список имен папок с одним элементом - именем папки с id = idDir
    fun getNameDir(idDir: Long): List<String>
    // Возвращает список id родительских папок с одним элементом - id родительской папки для папки с id = idDir
    fun getParentDir(idDir: Long): List<Long>

    // Возвращает список подчиненных записей для удаления
    fun selectionSubordinateRecordsToDelete(idDir: Long): List<ListRecord>
}