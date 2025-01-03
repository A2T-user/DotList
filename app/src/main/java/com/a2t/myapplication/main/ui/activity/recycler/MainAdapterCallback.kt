package com.a2t.myapplication.main.ui.activity.recycler

import androidx.recyclerview.widget.RecyclerView
import com.a2t.myapplication.main.domain.model.ListRecord
import com.a2t.myapplication.main.ui.activity.model.SpecialMode

interface MainAdapterCallback {
    fun onStartDrag(viewHolder: RecyclerView.ViewHolder)
    fun goToChildDir(id: Long)
    fun getIdCurrentDir(): Long
    fun insertNewRecord(item: ListRecord)
    fun updateRecord(record: ListRecord)
    fun requestMenuFocus()
    fun correctingPositionOfRecordByCheck(viewHolder: MainViewHolder)
    fun returnHolderToOriginalState(viewHolder: RecyclerView.ViewHolder)
    fun showContextMenuFormat(viewHolder: MainViewHolder)
    fun showContextMenuMove(viewHolder: MainViewHolder)
    fun updateFieldsOfSmallToolbar()
    fun deleteSingleRecord(records: List<ListRecord>)
    fun showNumberOfSelectedRecords()
    fun completionSpecialMode()
    fun getMoveBuffer(): ArrayList<ListRecord>
    fun getMainBuffer(): ArrayList<ListRecord>
    fun passRecordToAlarmFragment (record: ListRecord)
    fun setSpecialMode(mode: SpecialMode)
    fun enableSpecialMode()
}