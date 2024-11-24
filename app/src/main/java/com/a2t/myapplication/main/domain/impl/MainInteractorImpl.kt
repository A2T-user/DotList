package com.a2t.myapplication.main.domain.impl

import androidx.lifecycle.LiveData
import com.a2t.myapplication.main.domain.api.MainInteractor
import com.a2t.myapplication.main.domain.api.MainRepository
import com.a2t.myapplication.main.domain.model.ListRecord
import kotlinx.coroutines.flow.Flow

class MainInteractorImpl(
    private val mainRepository: MainRepository
): MainInteractor {

    override fun insertRecord(record: ListRecord): Long {
        return mainRepository.insertRecord(record)
    }

    override fun updateRecord(record: ListRecord) {
        mainRepository.updateRecord(record)
    }

    override fun getRecordsNormalMode(idDir: Long): Flow<List<ListRecord>> {
        return mainRepository.getRecordsNormalMode(idDir)
    }

    override fun getRecordsNormalModeSortedByCheck(idDir: Long): Flow<List<ListRecord>> {
        return mainRepository.getRecordsNormalModeSortedByCheck(idDir)
    }

    override fun getRecordsArchiveMode(idDir: Long): Flow<List<ListRecord>> {
        return mainRepository.getRecordsArchiveMode(idDir)
    }

    override fun getRecordsRestoreMode(idDir: Long): Flow<List<ListRecord>> {
        return mainRepository.getRecordsRestoreMode(idDir)
    }

    override fun getNameDir(idDir: Long): LiveData<List<String>> {
        return mainRepository.getNameDir(idDir)
    }

    override fun getParentDir(idDir: Long): LiveData<List<Long>> {
        return mainRepository.getParentDir(idDir)
    }
}