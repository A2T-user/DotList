package com.a2t.myapplication.main.ui.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction.TRANSIT_FRAGMENT_OPEN
import com.a2t.myapplication.App
import com.a2t.myapplication.R
import com.a2t.myapplication.databinding.FragmentToolbarSideBinding
import com.a2t.myapplication.main.presentation.MainViewModel
import com.a2t.myapplication.main.ui.activity.MainActivity
import com.a2t.myapplication.main.ui.SwipeGestureListener
import com.a2t.myapplication.main.ui.activity.model.SpecialMode
import com.a2t.myapplication.main.ui.fragments.models.TextFragmentMode
import com.a2t.myapplication.main.ui.utilities.AppHelper
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class ToolbarSideFragment: Fragment() {
    private val mainViewModel: MainViewModel by activityViewModel()
    private var _binding: FragmentToolbarSideBinding? = null
    private val binding get() = _binding!!
    private lateinit var ma:MainActivity
    private var isSideToolbarFullShow = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentToolbarSideBinding.inflate(layoutInflater)
        ma = requireActivity() as MainActivity
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sideBarGestureDetector = if (App.appSettings.isLeftHandControl) {
            GestureDetector(
                requireContext(),
                SwipeGestureListener(object : SwipeGestureListener.OnSwipeListener {
                    override fun onSwipeLeft(): Boolean {
                        sideBarHide()
                        return true
                    }
                    override fun onSwipeRight()= false
                    override fun onSwipeDown() = false
                })
            )
        } else {
            GestureDetector(
                requireContext(),
                SwipeGestureListener(object : SwipeGestureListener.OnSwipeListener {
                    override fun onSwipeLeft() = false
                    override fun onSwipeRight(): Boolean {
                        sideBarHide()
                        return true
                    }
                    override fun onSwipeDown() = false
                })
            )
        }

        // Кнопка Развернуть/Свернуть боковую панель
        binding.ivSideBarOpen.setOnClickListener {
            sideBarFullOpenClose()
        }
        binding.ivSideBarOpen.setOnTouchListener { _, event -> sideBarGestureDetector.onTouchEvent(event) }

        binding.tvSideBarOpen.setOnClickListener { sideBarFullOpenClose() }
        binding.tvSideBarOpen.setOnTouchListener { _, event -> sideBarGestureDetector.onTouchEvent(event) }

        // Кнопка не спящий режим
        binding.llSideBarNoSleep.setOnClickListener {
            AppHelper.requestFocusInTouch(binding.llSideBarNoSleep)
            ma.noSleepModeON()
        }
        binding.llSideBarNoSleep.setOnTouchListener { _, event -> sideBarGestureDetector.onTouchEvent(event) }

        // Кнопка Удалить метки
        binding.llSideBarDelMark.setOnClickListener {
            AppHelper.requestFocusInTouch(binding.llSideBarDelMark)
            ma.deleteAllMarks()
        }
        binding.llSideBarDelMark.setOnTouchListener { _, event -> sideBarGestureDetector.onTouchEvent(event) }

        // Кнопка Переслать
        binding.llSideBarSend.setOnClickListener {
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
                Toast.makeText(requireContext(), getString(R.string.dir_empty), Toast.LENGTH_SHORT).show()
            }
        }
        binding.llSideBarSend.setOnTouchListener { _, event -> sideBarGestureDetector.onTouchEvent(event) }

        // Кнопка Конвертировать
        binding.llSideBarConvertText.setOnClickListener {
            AppHelper.requestFocusInTouch(binding.llSideBarConvertText)
            mainViewModel.textFragmentMode = TextFragmentMode.CONVERT
            mainViewModel.idCurrentDir = ma.getIdCurrentDir()
            mainViewModel.mainRecords.clear()
            mainViewModel.mainRecords.addAll(ma.adapter.records)
            ma.fragmentManager.beginTransaction().setTransition(TRANSIT_FRAGMENT_OPEN)
                .add(R.id.container_view, TextFragment())
                .addToBackStack("textFragment").commit()
        }
        binding.llSideBarConvertText.setOnTouchListener { _, event -> sideBarGestureDetector.onTouchEvent(event) }

        // Кнопка режима Переноса
        binding.llSideBarMoveMode.setOnClickListener {
            AppHelper.requestFocusInTouch(binding.llSideBarMoveMode)
            ma.enableSpecialMode(SpecialMode.MOVE)
        }
        binding.llSideBarMoveMode.setOnTouchListener { _, event -> sideBarGestureDetector.onTouchEvent(event) }

        // Кнопка режима Удаления
        binding.llSideBarDelMode.setOnClickListener {
            AppHelper.requestFocusInTouch(binding.llSideBarDelMode)
            ma.enableSpecialMode(SpecialMode.DELETE)
        }
        binding.llSideBarDelMode.setOnTouchListener { _, event -> sideBarGestureDetector.onTouchEvent(event) }

        // Кнопка режима Восстановления
        binding.llSideBarRestMode.setOnClickListener {
            AppHelper.requestFocusInTouch(binding.llSideBarRestMode)
            ma.enableSpecialMode(SpecialMode.RESTORE)
        }
        binding.llSideBarRestMode.setOnTouchListener { _, event -> sideBarGestureDetector.onTouchEvent(event) }

        // Кнопка режима Архив
        binding.llSideBarArchiveMode.setOnClickListener {
            AppHelper.requestFocusInTouch(binding.llSideBarArchiveMode)
            ma.enableSpecialMode(SpecialMode.ARCHIVE)

        }
        binding.llSideBarArchiveMode.setOnTouchListener { _, event -> sideBarGestureDetector.onTouchEvent(event) }
    }

    private fun sideBarHide() {
        // Сворачивание боковой панели
        if (isSideToolbarFullShow) {
            binding.ivSideBarOpen.animate().rotation(0f)       // Перевернуть кнопку Развернуть панель
            showSideBarText(false)                             // Убрать пояснительный текст кнопок
        }
        // Закрытие фрагмента
        parentFragmentManager.beginTransaction().remove(this@ToolbarSideFragment).commitAllowingStateLoss()
        ma.sideBarFlagShow()
    }

    // Разворачивание/сворачивание боковой панели
    private fun sideBarFullOpenClose() {
        if (isSideToolbarFullShow) {
            binding.ivSideBarOpen.animate().rotation(0f)       // Перевернуть кнопку Развернуть панель
            showSideBarText(false)                             // Убрать пояснительный текст кнопок
        } else {
            binding.ivSideBarOpen.animate().rotation(180f)     // Перевернуть кнопку Развернуть панель
            showSideBarText(true)                              // Показать пояснительный текст кнопок
        }
        isSideToolbarFullShow = !isSideToolbarFullShow
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

    override fun onStart() {
        super.onStart()
        ma.mainBackPressedCallback.isEnabled = false
        ma.floatingBarBackPressedCallback.isEnabled = true
        ma.sideBarFlagHide()
    }

    override fun onResume() {
        super.onResume()
        binding.ivSideBarOpen.apply {
            isFocusable = true
            isFocusableInTouchMode = true
            requestFocus()
            setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    sideBarHide()
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        ma.floatingBarBackPressedCallback.isEnabled = false
        ma.mainBackPressedCallback.isEnabled = true
        //ma.sideBarFlagShow()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}