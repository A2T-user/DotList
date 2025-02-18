package com.a2t.myapplication.main.ui.activity

import android.util.Log
import com.a2t.myapplication.R
import com.a2t.myapplication.main.domain.model.ListRecord
import com.a2t.myapplication.main.presentation.MainViewModel
import com.a2t.myapplication.main.ui.activity.recycler.MainAdapter

class ContextMenuFormatManager(
    private val ma: MainActivity,
    private val adapter: MainAdapter,
    private val mainViewModel: MainViewModel
) {
    fun clickBtn(btnId: Int, item: ListRecord, position: Int) {
        Log.e("МОЁ", "clickBtn")
        when (btnId) {
            R.id.btnTextColor_1 -> changingTextFormatRecord(item, position, 1, null, null)
            R.id.btnTextColor_2 -> changingTextFormatRecord(item, position, 2, null, null)
            R.id.btnTextColor_3 -> changingTextFormatRecord(item, position, 3, null, null)
            R.id.btnTextStyle_B -> changingTextFormatRecord(item, position, null, 1, null)
            R.id.btnTextStyle_I -> changingTextFormatRecord(item, position, null, 2, null)
            R.id.btnTextStyle_BI -> changingTextFormatRecord(item, position, null, 3, null)
            R.id.btnTextStyle_U -> changingTextFormatRecord(item, position, null, null, 1)
            R.id.btnTextRegular -> changingTextFormatRecord(item, position, 0, 0, 0)
            else -> {}
        }
    }

    fun longClickBtn(btnId: Int, records: List<ListRecord>) {
        when (btnId) {
            R.id.btnTextColor_1 -> changingTextFormatAllRecords(records, 1, null, null)
            R.id.btnTextColor_2 -> changingTextFormatAllRecords(records, 2, null, null)
            R.id.btnTextColor_3 -> changingTextFormatAllRecords(records, 3, null, null)
            R.id.btnTextStyle_B -> changingTextFormatAllRecords(records, null, 1, null)
            R.id.btnTextStyle_I -> changingTextFormatAllRecords(records, null, 2, null)
            R.id.btnTextStyle_BI -> changingTextFormatAllRecords(records, null, 3, null)
            R.id.btnTextStyle_U -> changingTextFormatAllRecords(records, null, null, 1)
            R.id.btnTextRegular -> {
                changingTextFormatAllRecords(records, 0, 0, 0)
                ma.requestMenuFocus()
            }
            else -> {}
        }
    }

    private fun changingTextFormatRecord(item: ListRecord, position: Int, color: Int?, style: Int?, under: Int?) {
        if (color != null) item.textColor = color
        if (style != null) item.textStyle = style
        if (under != null) item.textUnder = under
        mainViewModel.updateRecord(item) {}
        adapter.notifyItemChanged(position)
    }

    private fun changingTextFormatAllRecords(records: List<ListRecord>, color: Int?, style: Int?, under: Int?) {
        records.forEachIndexed { index, listRecord ->
            changingTextFormatRecord(listRecord, index, color, style, under)
        }
    }
}