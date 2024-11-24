package com.a2t.myapplication.main.ui

import androidx.recyclerview.widget.RecyclerView
import com.a2t.myapplication.main.domain.model.ListRecord
import kotlinx.coroutines.Job

interface MainAdapterCallback {
    fun onStartDrag(viewHolder: RecyclerView.ViewHolder)
    fun goToChildDir (id: Long)
    fun getIdCurrentDir (): Long
    fun insertNewRecord(record: ListRecord): Job
    fun updateRecord(record: ListRecord)
}