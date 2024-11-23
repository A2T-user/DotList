package com.a2t.myapplication.main.domain.api

import com.a2t.myapplication.main.domain.model.ListRecord
import kotlinx.coroutines.flow.Flow

interface MainInteractor {
    /*fun insertRecord(record: ListRecord): LiveData<Long>
    fun updateRecord(record: ListRecord)*/
    fun getRecordsNormalMode(idDir: Long): Flow<List<ListRecord>>
    fun getRecordsNormalModeSortedByCheck(idDir: Long): Flow<List<ListRecord>>
    fun getRecordsArchiveMode(idDir: Long): Flow<List<ListRecord>>
    fun getRecordsRestoreMode(idDir: Long): Flow<List<ListRecord>>
    /*fun getNameDir(idDir: Long): LiveData<List<String>>
    fun getParentDir(idDir: Long): LiveData<List<Pair<Long, String>>>*/
}