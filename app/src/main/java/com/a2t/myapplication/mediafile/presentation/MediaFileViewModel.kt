package com.a2t.myapplication.mediafile.presentation

import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a2t.myapplication.common.App
import com.a2t.myapplication.mediafile.data.dto.DirType
import com.a2t.myapplication.mediafile.data.dto.MediaFileType
import com.a2t.myapplication.mediafile.data.dto.Response
import com.a2t.myapplication.mediafile.domaim.api.MediaFileInteractor
import com.a2t.myapplication.mediafile.domaim.model.MediaItem
import com.a2t.myapplication.mediafile.presentation.model.MediaFileFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MediaFileViewModel(
    private val mediaFileInteractor: MediaFileInteractor
): ViewModel() {
    var isLoadingLiveData = MutableLiveData(false)
    var baseListItem = mutableListOf<MediaItem>()
    var currentHolderItemLiveData = MutableLiveData<MediaItem?>(null)
    var filterLiveData = MutableLiveData(MediaFileFilter(DirType.GALLERY, null))
    var itemListLiveData = MutableLiveData<List<MediaItem>>(ArrayList())
    var resultAddingFileLiveData = MutableLiveData<MediaItem?>(null)
    var responseLiveData = MutableLiveData<Response>()

    fun getIsLoadingLiveData(): LiveData<Boolean> = isLoadingLiveData
    fun getFilterLiveData(): LiveData<MediaFileFilter> = filterLiveData
    fun getItemListLiveData(): LiveData<List<MediaItem>> = itemListLiveData
    fun getResultAddingFileLiveData(): LiveData<MediaItem?> = resultAddingFileLiveData
    fun getResponseLiveData(): LiveData<Response> = responseLiveData
    fun saveImageToPrivateStorage(sourceUri: Uri, mediaFileType: MediaFileType, deleteSourceAfterSave: Boolean, isNewCopy: Boolean) {
        viewModelScope.launch (Dispatchers.IO) {
            isLoadingLiveData.postValue(true)
            val response = mediaFileInteractor.saveImageToPrivateStorage(sourceUri,mediaFileType, deleteSourceAfterSave, isNewCopy)
            responseLiveData.postValue(response)
            isLoadingLiveData.postValue(false)
        }
    }
    fun getAllMediaFiles() {
        viewModelScope.launch (Dispatchers.IO) {
            isLoadingLiveData.postValue(true)
            val itemList = mediaFileInteractor.getAllMediaFiles()
            baseListItem.clear()
            baseListItem.addAll(itemList)
            filterListItems()
            isLoadingLiveData.postValue(false)
        }
    }
    fun filterListItems() {
        val filterList = baseListItem.filter { it.dir == filterLiveData.value!!.dir }
        if (filterLiveData.value!!.type != null) {
            val result = filterList.filter { it.mediaFileType == filterLiveData.value!!.type }
            itemListLiveData.postValue(result)
        } else {
            itemListLiveData.postValue(filterList)
        }
    }
    fun filterExistingFiles(originalName: String) {
        val filterList = baseListItem.filter { it.dir == DirType.APP }
        val result = mutableListOf<MediaItem>()
        for (item in filterList) {
            val uri = item.uri.toString()
            if (uri.endsWithFast(originalName)) {
                result.add(item)
            }
        }
        itemListLiveData.postValue(result)
    }
    // Ускоренная проверка окончаний для очень больших строк
    fun String.endsWithFast(suffix: String): Boolean {
        if (suffix.length > this.length) return false
        var i = this.length - 1
        var j = suffix.length - 1
        while (j >= 0) {
            if (this[i] != suffix[j]) return false
            i--
            j--
        }
        return true
    }

    fun addPhotoToInternalStorage(file: File) {
        isLoadingLiveData.postValue(true)
        val uri = FileProvider.getUriForFile(
            App.appContext,
            "com.a2t.myapplication.fileprovider",
            file
        )
        val dateFormat = SimpleDateFormat("dd.MM.yy", Locale.getDefault())
        val currentDate = dateFormat.format(Date())
        resultAddingFileLiveData.postValue(uri?.let {
            MediaItem(it, currentDate, MediaFileType.IMAGE, DirType.APP)
        })
        isLoadingLiveData.postValue(false)
    }

    fun addVideoToInternalStorage(file: File) {
        isLoadingLiveData.postValue(true)
        val uri = FileProvider.getUriForFile(
            App.appContext,
            "com.a2t.myapplication.fileprovider",
            file
        )
        val dateFormat = SimpleDateFormat("dd.MM.yy", Locale.getDefault())
        val currentDate = dateFormat.format(Date())
        resultAddingFileLiveData.postValue(uri?.let {
            MediaItem(it, currentDate, MediaFileType.VIDEO, DirType.APP)
        })
        isLoadingLiveData.postValue(false)
    }

    fun updateMediaFile(id: Long, fileName: String) {
        viewModelScope.launch (Dispatchers.IO) {
            mediaFileInteractor.updateMediaFile(id, fileName)
        }
    }
}