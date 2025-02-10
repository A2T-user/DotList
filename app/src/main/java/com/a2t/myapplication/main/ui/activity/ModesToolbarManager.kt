package com.a2t.myapplication.main.ui.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.widget.Toast
import com.a2t.myapplication.R
import com.a2t.myapplication.description.ui.DescriptionActivity
import com.a2t.myapplication.main.domain.model.ListRecord
import com.a2t.myapplication.main.presentation.MainViewModel
import com.a2t.myapplication.main.ui.activity.model.SpecialMode
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ModesToolbarManager(
    val context: Context,
    private val mainViewModel: MainViewModel
) {
    private val ma = context as MainActivity

    fun openHelp() {
        val currentTab = when(ma.adapter.specialMode) {
            SpecialMode.MOVE -> 10
            SpecialMode.DELETE -> 11
            SpecialMode.RESTORE -> 12
            SpecialMode.ARCHIVE -> 13
            else -> 0
        }
        if (currentTab != 0) openDescriptionActivity(currentTab)
    }

    private fun openDescriptionActivity(currentTab: Int) {
        val intent = Intent(context, DescriptionActivity::class.java)
        intent.putExtra(CURRENT_TAB, currentTab)
        context.startActivity(intent)
    }

    fun selectAll() {
        if (ma.getSpecialMode() == SpecialMode.DELETE || ma.getSpecialMode() == SpecialMode.RESTORE) {
            ma.adapter.records.forEachIndexed { index, rec ->
                if (!rec.isNew && getMainBuffer().all { it.id != rec.id }) {
                    getMainBuffer().add(rec)
                    ma.adapter.notifyItemChanged(index)
                }
            }
            ma.showNumberOfSelectedRecords()
        }
    }

    fun clickBtnAction() {
        when(ma.getSpecialMode()) {
            SpecialMode.MOVE -> {
                actionPasteRecords()
            }
            SpecialMode.DELETE -> {
                actionDeleteRecords()
            }
            SpecialMode.RESTORE -> {
                actionRestoreRecords()
            }
            else -> {}
        }
    }

    private fun getIdCurrentDir(): Long = mainViewModel.idDir

    private fun getMoveBuffer(): ArrayList<ListRecord> = mainViewModel.moveBuffer

    private fun getMainBuffer(): ArrayList<ListRecord> = mainViewModel.mainBuffer

    private fun actionPasteRecords() {
        if (getMainBuffer().size + getMoveBuffer().size > 0) {
            ma.showProgressbar(true)
            val pasteIds = mutableListOf<Long>()
            getMainBuffer().forEach { if (it.isDir) pasteIds.add(it.id) }
            getMoveBuffer().forEach { if (it.isDir) pasteIds.add(it.id) }
            if (pasteIds.isNotEmpty()) {
                mainViewModel.pasteRecords(getIdCurrentDir(), pasteIds,
                    {
                        ma.showProgressbar(false)
                        showRecursionError()
                    },
                    {
                        pasteRecords()
                        ma.showProgressbar(false)
                    }
                )
            } else {
                pasteRecords()
                ma.showProgressbar(false)
            }
        } else {
            Toast.makeText(context, context.getString(R.string.nothing_selected),
                Toast.LENGTH_SHORT).show()
        }
    }

    private fun actionDeleteRecords() {
        if (getMainBuffer().size > 0) {
            ma.deleteRecords(getMainBuffer())
        } else {
            Toast.makeText(context, context.getString(R.string.nothing_selected),
                Toast.LENGTH_SHORT).show()
        }
    }

    private fun actionRestoreRecords() {
        if (getMainBuffer().size > 0) {
            restoreRecords(getMainBuffer())
        } else {
            Toast.makeText(context, context.getString(R.string.nothing_selected),
                Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("InflateParams")
    private fun showRecursionError() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_title_error, null)
        MaterialAlertDialogBuilder(context)
            .setCustomTitle(dialogView)
            .setMessage(context.getString(R.string.recursion_error))
            .setPositiveButton(context.getString(R.string.ok)) { _, _ -> }
            .show()
    }

    private fun pasteRecords() {
        // Перенос записей
        getMoveBuffer().forEach { it.idDir = getIdCurrentDir() }
        mainViewModel.updateRecords(getMoveBuffer()) {
            // Копирование
            mainViewModel.copyRecords(getMainBuffer()){
                ma.completionSpecialMode()
            }
        }
    }

    @SuppressLint("InflateParams")
    fun restoreRecords(records: List<ListRecord>) {
        ma.showProgressbar(true)
        mainViewModel.selectionSubordinateRecordsToRestore(records) { list ->
            val mutableRecords = list.toMutableList()
            val selectedRecords = records.size
            val subordinateRecords = mutableRecords.size
            val countArchive = mutableRecords.count { it.isArchive }
            mutableRecords.addAll(records)
            var mess = context.getString(R.string.rest_attempt, selectedRecords.toString())
            var str = if (subordinateRecords != 0) context.getString(R.string.rest_subordinate, subordinateRecords.toString()) else ""
            mess += str
            str = if (countArchive != 0) context.getString(R.string.del_archive, countArchive.toString()) else ""
            mess += "$str."
            val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_title_attention, null)
            ma.showProgressbar(false)
            MaterialAlertDialogBuilder(context)
                .setCustomTitle(dialogView)
                .setMessage(mess)
                .setNeutralButton(context.getString(R.string.negative_btn)) { _, _ -> }
                .setPositiveButton(context.getString(R.string.restore)) { _, _ ->
                    mutableRecords.forEach { it.isDelete = false }
                    ma.showProgressbar(true)
                    mainViewModel.updateRecords(mutableRecords) {
                        ma.showProgressbar(false)
                        if (ma.getSpecialMode() == SpecialMode.RESTORE) ma.completionSpecialMode()
                    }
                }
                .show()
        }
    }

}