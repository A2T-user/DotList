package com.a2t.myapplication.root.presentation

import androidx.lifecycle.ViewModel
import com.a2t.myapplication.main.domain.model.ListRecord
import com.a2t.myapplication.root.presentation.model.TextFragmentMode

class SharedViewModel : ViewModel() {
    var textFragmentMode: TextFragmentMode? = null
    var idCurrentDir: Long = 0
    val mainRecords = ArrayList<ListRecord>()
}