package com.a2t.myapplication.main.ui.activity

import com.a2t.myapplication.R
import com.a2t.myapplication.main.domain.model.ListRecord
import com.a2t.myapplication.main.presentation.MainViewModel
import com.a2t.myapplication.main.ui.activity.recycler.MainAdapter

class ContextMenuFormatManager(
    private val ma: MainActivity,
    private val adapter: MainAdapter,
    private val mainViewModel: MainViewModel
) {
    fun clickBtn(btnId: Int) {
        when (btnId) {
            R.id.btnTextColor_1 -> changingTextFormatRecord(
                adapter.currentItem!!,
                adapter.currentHolderPosition,
                1,
                null,
                null
            )
            R.id.btnTextColor_2 -> changingTextFormatRecord(adapter.currentItem!!,
                adapter.currentHolderPosition,
                2,
                null,
                null
            )
            R.id.btnTextColor_3 -> changingTextFormatRecord(adapter.currentItem!!,
                adapter.currentHolderPosition,
                3
                , null
                , null
            )
            R.id.btnTextStyle_B -> changingTextFormatRecord(adapter.currentItem!!,
                adapter.currentHolderPosition,
                null,
                1,
                null
            )
            R.id.btnTextStyle_I -> changingTextFormatRecord(adapter.currentItem!!,
                adapter.currentHolderPosition,
                null,
                2,
                null
            )
            R.id.btnTextStyle_BI -> changingTextFormatRecord(adapter.currentItem!!,
                adapter.currentHolderPosition,
                null,
                3,
                null
            )
            R.id.btnTextStyle_U -> changingTextFormatRecord(adapter.currentItem!!,
                adapter.currentHolderPosition,
                null,
                null,
                1
            )
            R.id.btnTextRegular -> changingTextFormatRecord(adapter.currentItem!!,
                adapter.currentHolderPosition,
                0,
                0,
                0
            )
            else -> {}
        }
    }

    fun longClickBtn(btnId: Int) {
        when (btnId) {
            R.id.btnTextColor_1 -> changingTextFormatAllRecords(
                adapter.records,
                1,
                null,
                null
            )
            R.id.btnTextColor_2 -> changingTextFormatAllRecords(
                adapter.records,
                2,
                null,
                null
            )
            R.id.btnTextColor_3 -> changingTextFormatAllRecords(
                adapter.records,
                3,
                null,
                null
            )
            R.id.btnTextStyle_B -> changingTextFormatAllRecords(
                adapter.records,
                null,
                1,
                null
            )
            R.id.btnTextStyle_I -> changingTextFormatAllRecords(
                adapter.records,
                null,
                2,
                null
            )
            R.id.btnTextStyle_BI -> changingTextFormatAllRecords(
                adapter.records,
                null,
                3,
                null
            )
            R.id.btnTextStyle_U -> changingTextFormatAllRecords(
                adapter.records,
                null,
                null,
                1
            )
            R.id.btnTextRegular -> {
                changingTextFormatAllRecords(
                    adapter.records,
                    0,
                    0,
                    0
                )
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