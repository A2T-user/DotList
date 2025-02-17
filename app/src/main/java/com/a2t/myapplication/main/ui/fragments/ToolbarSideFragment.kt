package com.a2t.myapplication.main.ui.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.a2t.myapplication.App
import com.a2t.myapplication.databinding.FragmentToolbarSideBinding
import com.a2t.myapplication.main.presentation.MainViewModel
import com.a2t.myapplication.main.ui.activity.MainActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import kotlin.math.abs

class ToolbarSideFragment: Fragment() {
    private val mainViewModel: MainViewModel by activityViewModel()
    private var _binding: FragmentToolbarSideBinding? = null
    private val binding get() = _binding!!
    private lateinit var ma: MainActivity
    private var isSideToolbarFullShow = false
    private lateinit var tbManager: ToolbarSideManager
    private var isSwipeAllowed = true

    companion object {
        const val SWIPE_THRESHOLD = 10
    }
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
        // Получаем список View панели
        val btns = listOf(
            binding.root,
            binding.ivSideBarOpen,
            binding.tvSideBarOpen,
            binding.llSideBarNoSleep,
            binding.llSideBarDelMark,
            binding.llSideBarSend,
            binding.llSideBarConvertText,
            binding.llSideBarMoveMode,
            binding.llSideBarDelMode,
            binding.llSideBarRestMode,
            binding.llSideBarArchiveMode
        )

        tbManager = ToolbarSideManager(this, ma, mainViewModel, binding, isSideToolbarFullShow)

        if (App.appSettings.isLeftHandControl) binding.ivSideBarOpen.scaleX = -1.0f

        // Каждой View панели присваиваем слушателя
        var downX = 0f
        var downY = 0f
        var isSwipe = false
        for (btn in btns) {
            btn.setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        downX = event.x
                        downY = event.y
                        isSwipe = false
                    }
                    MotionEvent.ACTION_HOVER_EXIT, MotionEvent.ACTION_MOVE -> {
                        var dX = event.x - downX
                        val dY = event.y - downY
                        if (App.appSettings.isLeftHandControl) dX *= -1
                        if (abs(dX / dY) > 1 && dX > SWIPE_THRESHOLD) {         // Если жест горизонталный, влево
                            isSwipe = true
                            if (sideBarDebounce()) {
                                tbManager.sideBarHide()
                            }
                        }

                    }
                    MotionEvent.ACTION_UP -> {
                        if (!isSwipe) tbManager.clickBtn(btn.id)
                    }
                }
                return@setOnTouchListener true
            }
        }
    }

    private fun sideBarDebounce(): Boolean {
        val current = isSwipeAllowed
        if (isSwipeAllowed) {
            isSwipeAllowed = false
            lifecycleScope.launch {
                delay(1000)
                isSwipeAllowed = true
            }
        }
        return current
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