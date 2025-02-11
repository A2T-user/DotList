package com.a2t.myapplication.main.ui.activity

import com.a2t.myapplication.main.domain.model.ListRecord
import com.a2t.myapplication.main.presentation.MainViewModel
import com.a2t.myapplication.main.ui.activity.recycler.MainAdapter

class ContextMenuFormatManager(
    private val adapter: MainAdapter,
    private val mainViewModel: MainViewModel
) {
    fun changingTextFormatRecord(item: ListRecord, position: Int, color: Int?, style: Int?, under: Int?) {
        if (color != null) item.textColor = color
        if (style != null) item.textStyle = style
        if (under != null) item.textUnder = under
        mainViewModel.updateRecord(item) {}
        adapter.notifyItemChanged(position)
    }

    fun changingTextFormatAllRecords(records: List<ListRecord>, color: Int?, style: Int?, under: Int?) {
        records.forEachIndexed { index, listRecord ->
            changingTextFormatRecord(listRecord, index, color, style, under)
        }
    }
}