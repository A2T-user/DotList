package com.a2t.myapplication.main.ui.activity

import android.view.View
import com.a2t.myapplication.main.domain.model.ListRecord
import com.a2t.myapplication.main.presentation.MainViewModel
import com.a2t.myapplication.main.ui.activity.recycler.MainAdapter
import com.a2t.myapplication.main.ui.utilities.AppHelper

class ContextMenuMoveManager(
    private val ma: MainActivity,
    private val adapter: MainAdapter,
    private val mainViewModel: MainViewModel
) {
    private fun getMoveBuffer(): ArrayList<ListRecord> = mainViewModel.moveBuffer
    private fun getMainBuffer(): ArrayList<ListRecord> = mainViewModel.mainBuffer

    fun btnClick(view: View, option: String) {
        adapter.currentItem?.let { item ->
            removeItemFromBuffers(item)
            when(option) {
                "cut" -> getMoveBuffer().add(item)
                "copy" -> getMainBuffer().add(item)
                else -> {}
            }
            if (adapter.currentHolderPosition >= 0) {
                adapter.notifyItemChanged(adapter.currentHolderPosition)
                ma.showNumberOfSelectedRecords()
            }
        }
        AppHelper.requestFocusInTouch(view)
    }

    fun btnLongClick(view: View, option: String) {
        adapter.records.forEachIndexed { index, item ->
            removeItemFromBuffers(item)
            when(option) {
                "cut" -> getMoveBuffer().add(item)
                "copy" -> getMainBuffer().add(item)
                else -> {}
            }
            adapter.notifyItemChanged(index)
        }
        ma.showNumberOfSelectedRecords()
        AppHelper.requestFocusInTouch(view)
    }

    // Удалить item из буферов
    private fun removeItemFromBuffers(item: ListRecord) {
        getMainBuffer().removeAll { it.id == item.id }
        getMoveBuffer().removeAll { it.id == item.id }
    }
}