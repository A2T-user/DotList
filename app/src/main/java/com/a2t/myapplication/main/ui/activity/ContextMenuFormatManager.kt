package com.a2t.myapplication.main.ui.activity

import com.a2t.myapplication.R
import com.a2t.myapplication.main.presentation.MainViewModel
import com.a2t.myapplication.main.ui.activity.recycler.MainAdapter

class ContextMenuFormatManager(
    private val ma: MainActivity,
    private val adapter: MainAdapter,
    private val mainViewModel: MainViewModel
) {
    fun clickBtn(btnId: Int) {
        when (btnId) {
            R.id.btnTextColor_1 -> changingTextFormatRecord(1, null, null)
            R.id.btnTextColor_2 -> changingTextFormatRecord(2, null, null)
            R.id.btnTextColor_3 -> changingTextFormatRecord(3, null, null)
            R.id.btnTextStyle_B -> changingTextFormatRecord(null, 1, null)
            R.id.btnTextStyle_I -> changingTextFormatRecord(null, 2, null)
            R.id.btnTextStyle_BI -> changingTextFormatRecord(null, 3, null)
            R.id.btnTextStyle_U -> changingTextFormatRecord(null, null, 1)
            R.id.btnTextRegular -> changingTextFormatRecord(0, 0, 0)
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
                ma.requestMenuFocus()
            }
            else -> {}
        }
    }

    private fun changingTextFormatRecord(color: Int?, style: Int?, under: Int?) {
        if (color != null) adapter.currentItem!!.textColor = color
        if (style != null) adapter.currentItem!!.textStyle = style
        if (under != null) adapter.currentItem!!.textUnder = under
        mainViewModel.updateRecord(adapter.currentItem!!) {}
        adapter.notifyItemChanged(adapter.currentHolderPosition)
    }

    private fun changingTextFormatAllRecords(color: Int?, style: Int?, under: Int?) {
        adapter.records.forEachIndexed { index, listRecord ->
            changingTextFormatRecord(color, style, under)
        }
    }
}