package com.a2t.myapplication.main.ui.activity.managers

import com.a2t.myapplication.R
import com.a2t.myapplication.main.domain.model.ListRecord
import com.a2t.myapplication.main.presentation.MainViewModel
import com.a2t.myapplication.main.ui.activity.MainActivity
import com.a2t.myapplication.main.ui.activity.recycler.MainAdapter

class ContextMenuFormatManager(
    private val ma: MainActivity,
    private val adapter: MainAdapter,
    private val mainViewModel: MainViewModel
) {
    fun clickBtn(btnId: Int) {
        when (btnId) {
            R.id.btnTextColor_1 -> changingTextFormatCurrentRecord(1, null, null)
            R.id.btnTextColor_2 -> changingTextFormatCurrentRecord(2, null, null)
            R.id.btnTextColor_3 -> changingTextFormatCurrentRecord(3, null, null)
            R.id.btnTextStyle_B -> changingTextFormatCurrentRecord(null, 1, null)
            R.id.btnTextStyle_I -> changingTextFormatCurrentRecord(null, 2, null)
            R.id.btnTextStyle_BI -> changingTextFormatCurrentRecord(null, 3, null)
            R.id.btnTextStyle_U -> changingTextFormatCurrentRecord(null, null, 1)
            R.id.btnTextRegular -> changingTextFormatCurrentRecord(0, 0, 0)
            else -> {}
        }
    }

    fun longClickBtn(btnId: Int) {
        when (btnId) {
            R.id.btnTextColor_1 -> changingTextFormatAllRecords(1, null, null)
            R.id.btnTextColor_2 -> changingTextFormatAllRecords(2, null, null)
            R.id.btnTextColor_3 -> changingTextFormatAllRecords(3, null, null)
            R.id.btnTextStyle_B -> changingTextFormatAllRecords(null, 1, null)
            R.id.btnTextStyle_I -> changingTextFormatAllRecords(null, 2, null)
            R.id.btnTextStyle_BI -> changingTextFormatAllRecords(null, 3, null)
            R.id.btnTextStyle_U -> changingTextFormatAllRecords(null, null, 1)
            R.id.btnTextRegular -> {
                changingTextFormatAllRecords(0, 0, 0)
                ma.requestMenuFocus("ContextMenuFormatManager метод longClickBtn")
            }
            else -> {}
        }
        if (btnId != R.id.btnTextRegular) ma.hideContextMenuDebounce()
    }

    private fun changingTextFormatCurrentRecord(color: Int?, style: Int?, under: Int?) {
        changingTextFormatRecord(adapter.currentItem!!, adapter.currentHolderPosition, color, style, under)
        ma.hideContextMenuDebounce()
    }

    private fun changingTextFormatAllRecords(color: Int?, style: Int?, under: Int?) {
        adapter.records.forEachIndexed { index, listRecord ->
            changingTextFormatRecord(listRecord, index, color, style, under)
        }
    }

    private fun changingTextFormatRecord(item: ListRecord, position: Int, color: Int?, style: Int?, under: Int?) {
        if (color != null) item.textColor = color
        if (style != null) item.textStyle = style
        if (under != null) item.textUnder = under
        mainViewModel.updateRecord(item) {}
        adapter.notifyItemChanged(position)
    }
}