package com.a2t.myapplication.main.ui

import androidx.recyclerview.widget.RecyclerView

interface MainAdapterHelper {
    fun onStartDrag(viewHolder: RecyclerView.ViewHolder)
    /*fun goToChildDir (id: Long)
    fun getIdCurrentDir (): Long
    fun insertNewRecord(record: ListRecord)
    fun updateRecord(record: ListRecord)*/
}