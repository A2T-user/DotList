package com.a2t.myapplication.main.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.a2t.myapplication.main.domain.api.MainInteractor
import com.a2t.myapplication.main.domain.model.ListRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainViewModel (
    private val mainInteractor: MainInteractor
) : ViewModel() {

    suspend fun insertRecord(record: ListRecord)= withContext(Dispatchers.IO) {
        mainInteractor.insertRecord(record)
    }


    fun updateRecord(record: ListRecord) {
        mainInteractor.updateRecord(record)
    }



    // Возвращает список записей для режимов NORMAL, MOVE, DELETE
    suspend fun getRecordsForNormalMoveDeleteModes(idDir: Long)= withContext(Dispatchers.IO) {
        mainInteractor.getRecordsForNormalMoveDeleteModes(idDir)
    }
    // Возвращает список записей для режимов NORMAL, MOVE, DELETE с сортировкой по isChecked
    suspend fun getRecordsForNormalMoveDeleteModesByCheck(idDir: Long)= withContext(Dispatchers.IO) {
        mainInteractor.getRecordsForNormalMoveDeleteModesByCheck(idDir)
    }
    // Возвращает список записей для режимов RESTORE(isDelete = 1), ARCHIVE(isDelete = 0)
    suspend fun getRecordsForRestoreArchiveModes(idDir: Long, isDelete: Int)= withContext(Dispatchers.IO) {
        mainInteractor.getRecordsForRestoreArchiveModes(idDir, isDelete)
    }
    // Возвращает список записей для режимов RESTORE(isDelete = 1), ARCHIVE(isDelete = 0) с сортировкой по isChecked
    suspend fun getRecordsForRestoreArchiveModesByCheck(idDir: Long, isDelete: Int)= withContext(Dispatchers.IO) {
        mainInteractor.getRecordsForRestoreArchiveModesByCheck(idDir, isDelete)
    }


    fun getNameDir (idDir: Long): LiveData<List<String>> {
        return mainInteractor.getNameDir(idDir)
    }

    fun getParentDir(idDir: Long): LiveData<List<Long>> {
        return mainInteractor.getParentDir(idDir)
    }
}