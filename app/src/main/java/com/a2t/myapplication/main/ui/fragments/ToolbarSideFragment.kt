package com.a2t.myapplication.main.ui.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.a2t.myapplication.App
import com.a2t.myapplication.databinding.FragmentToolbarSideBinding
import com.a2t.myapplication.main.presentation.MainViewModel
import com.a2t.myapplication.main.ui.activity.MainActivity
import com.a2t.myapplication.main.ui.SwipeGestureListener
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class ToolbarSideFragment: Fragment() {
    private val mainViewModel: MainViewModel by activityViewModel()
    private var _binding: FragmentToolbarSideBinding? = null
    private val binding get() = _binding!!
    private lateinit var ma: MainActivity
    private var isSideToolbarFullShow = false
    private lateinit var tbManager: ToolbarSideManager

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

        tbManager = ToolbarSideManager(this, ma, mainViewModel, binding, isSideToolbarFullShow)

        if (App.appSettings.isLeftHandControl) binding.ivSideBarOpen.scaleX = -1.0f

        val sideBarGestureDetector = if (App.appSettings.isLeftHandControl) {
            GestureDetector(
                requireContext(),
                SwipeGestureListener(object : SwipeGestureListener.OnSwipeListener {
                    override fun onSwipeLeft(): Boolean {
                        tbManager.sideBarHide()
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
                        tbManager.sideBarHide()
                        return true
                    }
                    override fun onSwipeDown() = false
                })
            )
        }

        // Кнопка Развернуть/Свернуть боковую панель
        binding.ivSideBarOpen.setOnClickListener { tbManager.expandSideBar() }
        binding.ivSideBarOpen.setOnTouchListener { _, event -> sideBarGestureDetector.onTouchEvent(event) }
        binding.tvSideBarOpen.setOnClickListener { tbManager.expandSideBar() }
        binding.tvSideBarOpen.setOnTouchListener { _, event -> sideBarGestureDetector.onTouchEvent(event) }

        // Кнопка не спящий режим
        binding.llSideBarNoSleep.setOnClickListener { tbManager.noSleepMode() }
        binding.llSideBarNoSleep.setOnTouchListener { _, event -> sideBarGestureDetector.onTouchEvent(event) }

        // Кнопка Удалить метки
        binding.llSideBarDelMark.setOnClickListener { tbManager.delMark() }
        binding.llSideBarDelMark.setOnTouchListener { _, event -> sideBarGestureDetector.onTouchEvent(event) }

        // Кнопка Переслать
        binding.llSideBarSend.setOnClickListener { tbManager.sendList() }
        binding.llSideBarSend.setOnTouchListener { _, event -> sideBarGestureDetector.onTouchEvent(event) }

        // Кнопка Конвертировать
        binding.llSideBarConvertText.setOnClickListener { tbManager.convertText() }
        binding.llSideBarConvertText.setOnTouchListener { _, event -> sideBarGestureDetector.onTouchEvent(event) }

        // Кнопка режима Переноса
        binding.llSideBarMoveMode.setOnClickListener { tbManager.moveMode() }
        binding.llSideBarMoveMode.setOnTouchListener { _, event -> sideBarGestureDetector.onTouchEvent(event) }

        // Кнопка режима Удаления
        binding.llSideBarDelMode.setOnClickListener { tbManager.delMode() }
        binding.llSideBarDelMode.setOnTouchListener { _, event -> sideBarGestureDetector.onTouchEvent(event) }

        // Кнопка режима Восстановления
        binding.llSideBarRestMode.setOnClickListener { tbManager.restMode() }
        binding.llSideBarRestMode.setOnTouchListener { _, event -> sideBarGestureDetector.onTouchEvent(event) }

        // Кнопка режима Архив
        binding.llSideBarArchiveMode.setOnClickListener { tbManager.archiveMode() }
        binding.llSideBarArchiveMode.setOnTouchListener { _, event -> sideBarGestureDetector.onTouchEvent(event) }
    }


    override fun onStart() {
        super.onStart()
        ma.mainBackPressedCallback.isEnabled = false
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
                    tbManager.sideBarHide()
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        ma.mainBackPressedCallback.isEnabled = true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}