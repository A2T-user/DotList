package com.a2t.myapplication.main.ui.fragments

import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentTransaction.TRANSIT_FRAGMENT_OPEN
import com.a2t.myapplication.R
import com.a2t.myapplication.databinding.FragmentToolbarSideBinding
import com.a2t.myapplication.main.presentation.MainViewModel
import com.a2t.myapplication.main.ui.activity.MainActivity
import com.a2t.myapplication.main.ui.activity.model.SpecialMode
import com.a2t.myapplication.main.ui.fragments.models.TextFragmentMode
import com.a2t.myapplication.main.ui.utilities.AppHelper

class ToolbarSideManager (
    private val toolbarSideFragment: ToolbarSideFragment,
    private val ma: MainActivity,
    private val mainViewModel: MainViewModel,
    val binding: FragmentToolbarSideBinding,
    private var isSideToolbarFullShow: Boolean
){

    fun clickBtn(btnId: Int) {
        when(btnId) {
            R.id.ivSideBarOpen -> expandSideBar()
            R.id.tvSideBarOpen -> expandSideBar()
            R.id.llSideBarNoSleep -> noSleepMode()
            R.id.llSideBarDelMark -> delMark()
            R.id.llSideBarSend -> sendList()
            R.id.llSideBarConvertText -> convertText()
            R.id.llSideBarMoveMode -> moveMode()
            R.id.llSideBarDelMode -> delMode()
            R.id.llSideBarRestMode -> restMode()
            R.id.llSideBarArchiveMode -> archiveMode()
            else -> {}
        }
    }

    // Убрать боковую панель
    fun sideBarHide() {
        // Сворачивание боковой панели
        if (isSideToolbarFullShow) {
            binding.ivSideBarOpen.animate().rotation(0f)       // Перевернуть кнопку Развернуть панель
            showSideBarText(false)                             // Убрать пояснительный текст кнопок
        }
        // Закрытие фрагмента
        ma.supportFragmentManager.beginTransaction().remove(toolbarSideFragment).commitAllowingStateLoss()
        ma.sideBarFlagShow()
    }

    // Показать пояснительный текст кнопок боковой панели
    private fun showSideBarText(show: Boolean) {
        binding.tvSideBarOpen.isVisible = show                   // Текст кнопки Развернуть панель
        binding.tvSideBarNoSleep.isVisible = show                // Текст кнопки БЕЗ СНА
        binding.tvSideBarSend.isVisible = show                   // Текст кнопки Переслать
        binding.tvSideBarConvertText.isVisible = show            // Текст кнопки Конвертация
        binding.tvSideBarDelMark.isVisible = show                // Текст кнопки Удалить метки
        binding.tvSideBarDelMode.isVisible = show                // Текст кнопки Удаление
        binding.tvSideBarRestMode.isVisible = show               // Текст кнопки Восстановление
        binding.tvSideBarMoveMode.isVisible = show               // Текст кнопки Перенос
        binding.tvSideBarArchiveMode.isVisible = show            // Текст кнопки Архив
    }

    // Кнопка Развернуть/Свернуть боковую панель
    private fun expandSideBar() {
        if (isSideToolbarFullShow) {
            binding.ivSideBarOpen.animate().rotation(0f)       // Перевернуть кнопку Развернуть панель
            showSideBarText(false)                             // Убрать пояснительный текст кнопок
        } else {
            binding.ivSideBarOpen.animate().rotation(180f)     // Перевернуть кнопку Развернуть панель
            showSideBarText(true)                              // Показать пояснительный текст кнопок
        }
        isSideToolbarFullShow = !isSideToolbarFullShow
    }

    // Кнопка не спящий режим
    private fun noSleepMode() {
        AppHelper.requestFocusInTouch(binding.llSideBarNoSleep)
        ma.noSleepModeON()
    }

    // Кнопка Удалить метки
    private fun delMark() {
        AppHelper.requestFocusInTouch(binding.llSideBarDelMark)
        ma.deleteAllMarks()
    }

    // Кнопка Переслать
    private fun sendList() {
        AppHelper.requestFocusInTouch(binding.llSideBarSend)
        if (ma.adapter.records.size > 1) {
            mainViewModel.textFragmentMode = TextFragmentMode.SEND
            mainViewModel.idCurrentDir = ma.getIdCurrentDir()
            mainViewModel.mainRecords.clear()
            mainViewModel.mainRecords.addAll(ma.adapter.records)
            ma.fragmentManager.beginTransaction().setTransition(TRANSIT_FRAGMENT_OPEN)
                .add(R.id.container_view, TextFragment())
                .addToBackStack("textFragment").commit()
        } else {
            Toast.makeText(ma, ma.getString(R.string.dir_empty), Toast.LENGTH_SHORT).show()
        }
    }

    // Кнопка Конвертировать
    private fun convertText() {
        AppHelper.requestFocusInTouch(binding.llSideBarConvertText)
        mainViewModel.textFragmentMode = TextFragmentMode.CONVERT
        mainViewModel.idCurrentDir = ma.getIdCurrentDir()
        mainViewModel.mainRecords.clear()
        mainViewModel.mainRecords.addAll(ma.adapter.records)
        ma.fragmentManager.beginTransaction().setTransition(TRANSIT_FRAGMENT_OPEN)
            .add(R.id.container_view, TextFragment())
            .addToBackStack("textFragment").commit()
    }

    // Кнопка режима Переноса
    private fun moveMode() {
        AppHelper.requestFocusInTouch(binding.llSideBarMoveMode)
        ma.enableSpecialMode(SpecialMode.MOVE)
    }

    // Кнопка режима Удаления
    private fun delMode() {
        AppHelper.requestFocusInTouch(binding.llSideBarDelMode)
        ma.enableSpecialMode(SpecialMode.DELETE)
    }

    // Кнопка режима Восстановления
    private fun restMode() {
        AppHelper.requestFocusInTouch(binding.llSideBarRestMode)
        ma.enableSpecialMode(SpecialMode.RESTORE)
    }

    // Кнопка режима Архив
    private fun archiveMode() {
        AppHelper.requestFocusInTouch(binding.llSideBarArchiveMode)
        ma.enableSpecialMode(SpecialMode.ARCHIVE)
    }
}