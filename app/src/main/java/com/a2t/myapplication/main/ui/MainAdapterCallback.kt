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
    fun requestEyeFocus ()
    fun correctingPositionOfRecordByCheck (viewHolder: MainViewHolder)
    fun returnHolderToOriginalState(viewHolder: RecyclerView.ViewHolder)
    fun showContextMenuFormat(viewHolder: MainViewHolder)
    fun showContextMenuMove(viewHolder: MainViewHolder)
    fun updatFieldsOfSmallToolbar ()
    fun deleteRecord (records: List<ListRecord>)
}