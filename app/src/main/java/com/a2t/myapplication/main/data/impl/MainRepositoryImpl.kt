package com.a2t.myapplication.main.data.impl

import androidx.lifecycle.LiveData
import com.a2t.myapplication.common.data.db.AppDatabase
import com.a2t.myapplication.main.data.RecordDBConverter
import com.a2t.myapplication.main.data.entity.ListRecordEntity
import com.a2t.myapplication.main.domain.api.MainRepository
import com.a2t.myapplication.main.domain.model.ListRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
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

    // Получение массива списка. Режим NORMAL, MOVE, DELETE
    override fun getRecordsNormalMode(idDir: Long): Flow<List<ListRecord>> = flow {
        val records = appDatabase.mainRecordDao().getRecordsNormalMode(idDir)
        emit(convertFromListRecordEntityNormal(records))
    }

    // Получение массива списка. Режим NORMAL, MOVE, DELETE с сортировкой меток
    override fun getRecordsNormalModeSortedByCheck(idDir: Long): Flow<List<ListRecord>> = flow {
        val records = appDatabase.mainRecordDao().getRecordsNormalModeSortedByCheck(idDir)
        emit(convertFromListRecordEntityNormal(records))
    }

    private suspend fun convertFromListRecordEntityNormal(records: List<ListRecordEntity>): List<ListRecord> {
        return records.map { record ->  convertInListRecordNormal(record) }
    }

    private suspend fun convertInListRecordNormal(recordEntity: ListRecordEntity): ListRecord {
        val record =  recordDBConverter.map(recordEntity)
        val records = appDatabase.mainRecordDao().getRecordsNormalMode(record.id)
        if (records.isNotEmpty()) record.isFull = true
        if (records.all { it.isChecked }) record.isAllCheck = true
        return record
    }
    // Получение массива списка. Режим ARCHIVE
    override fun getRecordsArchiveMode(idDir: Long): Flow<List<ListRecord>> = flow {
        val records = appDatabase.mainRecordDao().getRecordsArchiveMode(idDir)
        emit(convertFromListRecordEntityArchive(records))
    }

    private suspend fun convertFromListRecordEntityArchive(records: List<ListRecordEntity>): List<ListRecord> {
        return records.map { record ->  convertInListRecordArchive(record) }
    }

    private suspend fun convertInListRecordArchive(recordEntity: ListRecordEntity): ListRecord {
        val record =  recordDBConverter.map(recordEntity)
        val records = appDatabase.mainRecordDao().getRecordsArchiveMode(record.id)
        if (records.isNotEmpty()) record.isFull = true
        if (records.all { it.isChecked }) record.isAllCheck = true
        return record
    }

    // Получение массива списка. Режим RESTORE
    override fun getRecordsRestoreMode(idDir: Long): Flow<List<ListRecord>> = flow {
        val records = appDatabase.mainRecordDao().getRecordsRestoreMode(idDir)
        emit(convertFromListRecordEntityRestore(records))
    }

    private suspend fun convertFromListRecordEntityRestore(records: List<ListRecordEntity>): List<ListRecord> {
        return records.map { record ->  convertInListRecordRestore(record) }
    }

    private suspend fun convertInListRecordRestore(recordEntity: ListRecordEntity): ListRecord {
        val record =  recordDBConverter.map(recordEntity)
        val records = appDatabase.mainRecordDao().getRecordsRestoreMode(record.id)
        if (records.isNotEmpty()) record.isFull = true
        if (records.all { it.isChecked }) record.isAllCheck = true
        return record
    }

    // Получение имени папки
    override fun getNameDir(idDir: Long): LiveData<List<String>> {
        return appDatabase.mainRecordDao().getNameDir(idDir)
    }

    // Получение id и имени родительской папки
    override fun getParentDir(idDir: Long): LiveData<List<Long>> {
        return appDatabase.mainRecordDao().getParentDir(idDir)
    }
}