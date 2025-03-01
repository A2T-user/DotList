package com.a2t.myapplication.main.ui.activity.managers

import com.a2t.myapplication.R
import com.a2t.myapplication.main.domain.model.ListRecord
import com.a2t.myapplication.main.presentation.MainViewModel
import com.a2t.myapplication.main.ui.activity.MainActivity
import com.a2t.myapplication.main.ui.activity.recycler.MainAdapter

class ContextMenuMoveManager(
    private val ma: MainActivity,
    private val adapter: MainAdapter,
    private val mainViewModel: MainViewModel
) {
    private fun getMoveBuffer(): ArrayList<ListRecord> = mainViewModel.moveBuffer
    private fun getMainBuffer(): ArrayList<ListRecord> = mainViewModel.mainBuffer

    fun clickBtn(btnId: Int) {
        when (btnId) {
            R.id.btnCut -> changeStateRecord("cut")
            R.id.btnCopy -> changeStateRecord("copy")
            R.id.btnBack -> changeStateRecord("cancel")
        }
    }

    fun longClickBtn(btnId: Int) {
        when (btnId) {
            R.id.btnCut -> changeStateAllRecords("cut")
            R.id.btnCopy -> changeStateAllRecords("copy")
            R.id.btnBack -> changeStateAllRecords("cancel")
        }
    }

    private fun changeStateRecord(option: String) {
        adapter.currentItem?.let { item ->
            changeStateItem(item, option)
            adapter.notifyItemChanged(adapter.currentHolderPosition)
            ma.showNumberOfSelectedRecords()
        }
        ma.requestMenuFocus("ContextMenuMoveManager метод changeStateRecord")
    }

    private fun changeStateAllRecords(option: String) {
        adapter.records.forEachIndexed { index, item ->
            changeStateItem(item, option)
            adapter.notifyItemChanged(index)
        }
        ma.showNumberOfSelectedRecords()
        ma.requestMenuFocus("ContextMenuMoveManager метод changeStateRecord")
    }

    private fun changeStateItem(item: ListRecord, option: String) {
        removeItemFromBuffers(item)
        when(option) {
            "cut" -> getMoveBuffer().add(item)
            "copy" -> getMainBuffer().add(item)
            else -> {}
        }
    }

    // Удалить item из буферов
    private fun removeItemFromBuffers(item: ListRecord) {
        getMainBuffer().removeAll { it.id == item.id }
        getMoveBuffer().removeAll { it.id == item.id }
    }
}