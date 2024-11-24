package com.a2t.myapplication.main.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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
    private var mainLiveData = MutableLiveData(mutableListOf<ListRecord>())
    //private var nameLiveData = MutableLiveData<String>()

    fun getMainLiveData(): LiveData<MutableList<ListRecord>> = mainLiveData
    //fun getNameLiveData(): LiveData<String> = nameLiveData

    fun getRecords (specialMode: SpecialMode, idDir: Long) {
        // getNameDir(idDir)
        when (specialMode) {
            SpecialMode.NORMAL, SpecialMode.MOVE, SpecialMode.DELETE -> {
                if (App.appSettings.sortingChecks) {
                    getRecordsNormalModeSortedByCheck(specialMode, idDir)
                } else {
                    getRecordsNormalMode(specialMode, idDir)
                }
            }
            SpecialMode.ARCHIVE -> getRecordsArchiveMode(specialMode, idDir)
            SpecialMode.RESTORE -> getRecordsRestoreMode(specialMode, idDir)
        }
    }

    private fun getRecordsNormalMode(specialMode: SpecialMode, idDir: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            mainInteractor
                .getRecordsNormalMode(idDir)
                .collect { mainLiveData.postValue(addNewRecords(idDir, it, specialMode)) }
        }
    }

    private fun getRecordsNormalModeSortedByCheck (specialMode: SpecialMode, idDir: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            mainInteractor
                .getRecordsNormalModeSortedByCheck(idDir)
                .collect { mainLiveData.postValue(addNewRecords(idDir, it, specialMode)) }
        }
    }

    private fun getRecordsArchiveMode(specialMode: SpecialMode, idDir: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            mainInteractor
                .getRecordsNormalMode(idDir)
                .collect { mainLiveData.postValue(addNewRecords(idDir, it, specialMode)) }
        }
    }

    private fun getRecordsRestoreMode(specialMode: SpecialMode, idDir: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            mainInteractor
                .getRecordsNormalMode(idDir)
                .collect { mainLiveData.postValue(addNewRecords(idDir, it, specialMode)) }
        }
    }

    private fun addNewRecords (idDir: Long, records: List<ListRecord>, specialMode: SpecialMode): MutableList<ListRecord> {
        val mutableRecords = records.toMutableList()
        if (specialMode == SpecialMode.NORMAL) {
            mutableRecords.add(getNewRecord(idDir, records,records.isEmpty() && App.appSettings.editEmptyDir))
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

    fun getNameDir (idDir: Long): LiveData<List<String>> {
        return mainInteractor.getNameDir(idDir)
    }

    fun getParentDir(idDir: Long): LiveData<List<Long>> {
        return mainInteractor.getParentDir(idDir)
    }

    suspend fun insertRecord(record: ListRecord)= withContext(Dispatchers.IO) {
        mainInteractor.insertRecord(record)
    }


    fun updateRecord(record: ListRecord) {
        mainInteractor.updateRecord(record)
    }

}