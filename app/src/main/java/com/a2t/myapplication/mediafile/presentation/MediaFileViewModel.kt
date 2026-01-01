package com.a2t.myapplication.mediafile.presentation

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a2t.myapplication.mediafile.data.model.MediaType
import com.a2t.myapplication.mediafile.domaim.api.MediaFileInteractor
import com.a2t.myapplication.mediafile.domaim.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class MediaFileViewModel(
    private val mediaFileInteractor: MediaFileInteractor
): ViewModel() {
    var currentHolderUriLiveData = MutableLiveData<Uri?>(null)
    var selectedTypeLiveData = MutableLiveData(MediaType.ALL)
    var itemListLiveData = MutableLiveData<List<MediaItem>>(ArrayList())
    var resultAddingFileLiveData = MutableLiveData<Uri?>(null)

    fun getSelectedTypeLiveData(): LiveData<MediaType> = selectedTypeLiveData
    fun getItemListLiveData(): LiveData<List<MediaItem>> = itemListLiveData
    fun getResultAddingFileLiveData(): LiveData<Uri?> = resultAddingFileLiveData
    fun getAllMediaFiles() {
        val type: MediaType = selectedTypeLiveData.value!!
        viewModelScope.launch (Dispatchers.IO) {
            val itemList = mediaFileInteractor.getAllMediaFiles(type)
            itemListLiveData.postValue(itemList)
        }
    }
    fun addPhotoToGallery(file: File) {
        viewModelScope.launch (Dispatchers.IO) {
            val uri = mediaFileInteractor.addPhotoToGallery(file)
            resultAddingFileLiveData.postValue(uri)
        }
    }
    fun addVideoToGallery(file: File) {
        viewModelScope.launch (Dispatchers.IO) {
            val uri = mediaFileInteractor.addVideoToGallery(file)
            resultAddingFileLiveData.postValue(uri)
        }
    }
}