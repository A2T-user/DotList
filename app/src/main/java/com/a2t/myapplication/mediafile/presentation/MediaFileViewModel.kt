package com.a2t.myapplication.mediafile.presentation

import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a2t.myapplication.common.App
import com.a2t.myapplication.common.utilities.AppHelper
import com.a2t.myapplication.mediafile.data.dto.DirType
import com.a2t.myapplication.mediafile.data.dto.MediaFileType
import com.a2t.myapplication.mediafile.data.dto.Response
import com.a2t.myapplication.mediafile.domaim.api.MediaFileInteractor
import com.a2t.myapplication.mediafile.domaim.model.MediaItem
import com.a2t.myapplication.mediafile.presentation.model.MediaFileFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MediaFileViewModel(
    private val mediaFileInteractor: MediaFileInteractor
): ViewModel() {
    var copyJob = MutableLiveData<Job?>(null)
    var isLoadingLiveData = MutableLiveData(false)
    var baseListItem = mutableListOf<MediaItem>()
    var currentHolderItemLiveData = MutableLiveData<MediaItem?>(null)
    var filterLiveData = MutableLiveData(MediaFileFilter(DirType.GALLERY, null))
    var itemListLiveData = MutableLiveData<List<MediaItem>>(ArrayList())
    var resultAddingFileLiveData = MutableLiveData<MediaItem?>(null)
    var responseCopyToPrivateStorageLiveData = MutableLiveData<Response>()
    var responseCopyToPublicStorageLiveData = MutableLiveData<Response>()

    fun getIsLoadingLiveData(): LiveData<Boolean> = isLoadingLiveData
    fun getFilterLiveData(): LiveData<MediaFileFilter> = filterLiveData
    fun getItemListLiveData(): LiveData<List<MediaItem>> = itemListLiveData
    fun getResultAddingFileLiveData(): LiveData<MediaItem?> = resultAddingFileLiveData
    fun getResponseCopyToPrivateStorageLiveData(): LiveData<Response> = responseCopyToPrivateStorageLiveData
    fun getResponseCopyToPublicStorageLiveData(): LiveData<Response> = responseCopyToPublicStorageLiveData
    fun saveImageToExternalAppStorage(sourceUri: Uri, mediaFileType: MediaFileType, dir: DirType, isNewCopy: Boolean) {
        viewModelScope.launch (Dispatchers.IO) {
            isLoadingLiveData.postValue(true)
            val response = mediaFileInteractor.saveFileToExternalAppStorage(sourceUri,mediaFileType, dir, isNewCopy)
            responseCopyToPrivateStorageLiveData.postValue(response)
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
            if (result.isNotEmpty()) currentHolderItemLiveData.postValue(result[0])

        } else {
            itemListLiveData.postValue(filterList)
            if (filterList.isNotEmpty()) currentHolderItemLiveData.postValue(filterList[0])
        }
    }
    fun filterExistingFiles(originalName: String, mediaFileType: MediaFileType) {
        val filterList = baseListItem.filter {
            it.dir == DirType.APP && it.mediaFileType == mediaFileType
        }
        val result = mutableListOf<MediaItem>()
        for (item in filterList) {
            val fileName = getFileName(item.uri)
            if (fileName.endsWithFast(originalName)) {
                result.add(item)
            }
        }
        itemListLiveData.postValue(result)
    }
    private fun getFileName(uri: Uri): String {
        val fileName = AppHelper.getFileNameFromUri(uri)
        // отделяем наш префикс
        return fileName?.substringAfterLast("#") ?: ""
    }
    // Ускоренная проверка окончаний для больших строк
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
    // Добавление файла во внешнее хранилище приложения
    fun addPhotoToExternalAppStorage(uri: Uri) {
        isLoadingLiveData.postValue(true)
        val dateFormat = SimpleDateFormat("dd.MM.yy", Locale.getDefault())
        val currentDate = dateFormat.format(Date())
        resultAddingFileLiveData.postValue(
            MediaItem(uri, currentDate, MediaFileType.IMAGE, DirType.GALLERY)
        )
        isLoadingLiveData.postValue(false)
    }

    fun updateMediaFile(id: Long, fileName: String) {
        viewModelScope.launch (Dispatchers.IO) {
            mediaFileInteractor.updateMediaFile(id, fileName)
        }
    }
    // Сохранение медиафайла из внешнего хранилища приложения в общем хранилище
    fun copyFileFromExternalAppToPublicStorage(sourceUri: Uri, mediaFileType: MediaFileType) {
        isLoadingLiveData.postValue(true)
        copyJob.value = viewModelScope.launch (Dispatchers.IO) {
            val response = mediaFileInteractor.copyFileFromExternalAppToPublicStorage(sourceUri, mediaFileType)
            responseCopyToPublicStorageLiveData.postValue(response)
            copyJob.postValue(null)
            isLoadingLiveData.postValue(false)
        }
    }

    fun isTheFileInPublicStorage(fileName: String): Boolean {
        val uri = MediaStore.Files.getContentUri("external")
        val selection = "${MediaStore.Files.FileColumns.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(fileName)
        App.appContext.contentResolver.query(
            uri,
            arrayOf(MediaStore.Files.FileColumns.DATA),
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            return cursor.moveToFirst()
        }
        return false
    }
}