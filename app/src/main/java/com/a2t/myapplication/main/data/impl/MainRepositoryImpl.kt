package com.a2t.myapplication.main.data.impl

import androidx.lifecycle.LiveData
import com.a2t.myapplication.common.data.db.AppDatabase
import com.a2t.myapplication.main.data.RecordDBConverter
import com.a2t.myapplication.main.data.entity.ListRecordEntity
import com.a2t.myapplication.main.domain.api.MainRepository
import com.a2t.myapplication.main.domain.model.ListRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainRepositoryImpl(
    private val appDatabase: AppDatabase,
    private val recordDBConverter: RecordDBConverter
): MainRepository {

    // Добавление новой записи и получение ее id
    override fun insertRecord(record: ListRecord): Long {
        return appDatabase.mainRecordDao().insertRecord(recordDBConverter.map(record))
    }
    // Обновление записи
    override fun updateRecord(record: ListRecord) {
        CoroutineScope(Dispatchers.IO).launch {
            appDatabase.mainRecordDao().updateRecord(recordDBConverter.map(record))
        }
    }
    // Обновление записей
    override fun updateRecords(records: List<ListRecord>) {
        CoroutineScope(Dispatchers.IO).launch {
            appDatabase.mainRecordDao().updateRecords(records.map { record -> recordDBConverter.map(record) })
        }
    }


    // NORMAL, MOVE, DELETE
    // Возвращает список записей для режимов NORMAL, MOVE, DELETE
    override fun getRecordsForNormalMoveDeleteModes(idDir: Long): List<ListRecord> {
        return convertFromListRecordEntityForNormalMoveDeleteModes(
            appDatabase.mainRecordDao().getRecordsForNormalMoveDeleteModes(idDir)
        )
    }
    // Возвращает список записей для режимов NORMAL, MOVE, DELETE с сортировкой по isChecked
    override fun getRecordsForNormalMoveDeleteModesByCheck(idDir: Long): List<ListRecord> {
        return convertFromListRecordEntityForNormalMoveDeleteModes(
            appDatabase.mainRecordDao().getRecordsForNormalMoveDeleteModesByCheck(idDir)
        )
    }
    private  fun convertFromListRecordEntityForNormalMoveDeleteModes(records: List<ListRecordEntity>): List<ListRecord> {
        return records.map { record ->  convertInListRecordForNormalMoveDeleteModes(record) }
    }
    private fun convertInListRecordForNormalMoveDeleteModes(recordEntity: ListRecordEntity): ListRecord {
        val record =  recordDBConverter.map(recordEntity)
        val records = appDatabase.mainRecordDao().getRecordsForNormalMoveDeleteModes(record.id)
        if (records.isNotEmpty()) record.isFull = true
        if (records.all { it.isChecked }) record.isAllCheck = true
        return record
    }

    // RESTORE, ARCHIVE
    // Возвращает список записей для режимов RESTORE(isDelete = 1), ARCHIVE(isDelete = 0)
    override fun getRecordsForRestoreArchiveModes(idDir: Long, isDelete: Int): List<ListRecord> {
        return convertFromListRecordEntityForRestoreArchiveModes(
            appDatabase.mainRecordDao().getRecordsForRestoreArchiveModes(idDir, isDelete),
            isDelete
        )
    }
    // Возвращает список записей для режимов RESTORE(isDelete = 1), ARCHIVE(isDelete = 0) с сортировкой по isChecked
    override fun getRecordsForRestoreArchiveModesByCheck(idDir: Long, isDelete: Int): List<ListRecord> {
        return convertFromListRecordEntityForRestoreArchiveModes(
            appDatabase.mainRecordDao().getRecordsForRestoreArchiveModesByCheck(idDir, isDelete),
            isDelete
        )
    }
    private  fun convertFromListRecordEntityForRestoreArchiveModes(records: List<ListRecordEntity>, isDelete: Int): List<ListRecord> {
        return records.map { record ->  convertInListRecordForRestoreArchiveModes(record, isDelete) }
    }
    private fun convertInListRecordForRestoreArchiveModes(recordEntity: ListRecordEntity, isDelete: Int): ListRecord {
        val record =  recordDBConverter.map(recordEntity)
        val records = appDatabase.mainRecordDao().getRecordsForRestoreArchiveModes(record.id, isDelete)
        if (records.isNotEmpty()) record.isFull = true
        if (records.all { it.isChecked }) record.isAllCheck = true
        return record
    }

    // Возвращает список имен папок с одним элементом - именем папки с id = idDir
    override fun getNameDir(idDir: Long): LiveData<List<String>> {
        return appDatabase.mainRecordDao().getNameDir(idDir)
    }

    // Возвращает список id родительских папок с одним элементом - id родительской папки для папки с id = idDir
    override fun getParentDir(idDir: Long): LiveData<List<Long>> {
        return appDatabase.mainRecordDao().getParentDir(idDir)
    }
}