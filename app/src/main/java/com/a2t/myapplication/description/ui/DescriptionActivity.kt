package com.a2t.myapplication.description.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.GestureDetector
import android.view.View
import android.widget.ScrollView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.viewpager2.widget.ViewPager2
import com.a2t.myapplication.databinding.ActivityDescriptionBinding
import com.a2t.myapplication.databinding.DescriptionContentBinding
import com.a2t.myapplication.main.ui.activity.SwipeGestureListener
import com.a2t.myapplication.main.ui.utilities.AppHelper

const val CURRENT_TAB = "current_tab"

class DescriptionActivity : AppCompatActivity() {
    private lateinit var descBackPressedCallback: OnBackPressedCallback
    private var _binding: ActivityDescriptionBinding? = null
    private val binding get() = _binding!!
    private lateinit var descriptionContentBinding: DescriptionContentBinding
    private lateinit var viewPager: ViewPager2
    lateinit var currentScrollView: ScrollView
    private lateinit var gestureDetector: GestureDetector

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityDescriptionBinding.inflate(layoutInflater)
        descriptionContentBinding = binding.descContent
        setContentView(binding.root)

        // $$$$$$$$$$$$$$$$$$$$$$   Реакция на нажатие системной кнопки BACK   $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$
        descBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                AppHelper.requestFocusInTouch(descriptionContentBinding.descGeneral1)
            }
        }

        descBackPressedCallback.isEnabled = false
        onBackPressedDispatcher.addCallback(this, descBackPressedCallback)

        viewPager = binding.pager
        val adapter = PagerAdapter(this)
        viewPager.adapter = adapter

        val currentTab = savedInstanceState?.getInt(CURRENT_TAB, 0) // Значение по умолчанию - 0
            ?: intent.getIntExtra(CURRENT_TAB, 0)
        goToTab(currentTab, false)

        gestureDetector = GestureDetector(this, SwipeGestureListener(object : SwipeGestureListener.OnSwipeListener {
            override fun onSwipeLeft(): Boolean {
                closeDescriptionContent()
                return true
            }
            override fun onSwipeRight() = false
            override fun onSwipeDown() = false
        }))

        binding.goToPrevious.setOnClickListener {
            goToTab(viewPager.currentItem, true)
        }
        binding.goToPrevious.setOnLongClickListener {
            goToTab(1, true)
            true
        }

        binding.goToNext.setOnClickListener {
            goToTab(viewPager.currentItem + 2, true)
        }
        binding.goToNext.setOnLongClickListener {
            goToTab(15, true)
            true
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                when(position) {
                    0 -> {
                        binding.goToPrevious.alpha = 0.5f
                        binding.goToNext.alpha = 1f
                    }
                    14 -> {
                        binding.goToNext.alpha = 0.5f
                        binding.goToPrevious.alpha = 1f
                    }
                    else -> {
                        binding.goToPrevious.alpha = 1f
                        binding.goToNext.alpha = 1f
                    }
                }
            }
        })

        binding.goToDescContent.setOnClickListener {
            descriptionContentBinding.llDescriptionContent.isVisible = true
            descriptionContentBinding.llDescriptionContent.requestFocus()
            descBackPressedCallback.isEnabled = true
        }

        descriptionContentBinding.llDescriptionContent.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) closeDescriptionContent()
        }

        descriptionContentBinding.llDescriptionContent.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
        }

        binding.fon.setOnTouchListener { v, _ ->
            AppHelper.requestFocusInTouch(v)
            true
        }

        createListeners(descriptionContentBinding.descGeneral1, 1)
        createListeners(descriptionContentBinding.records2, 2)
        createListeners(descriptionContentBinding.lines3, 3)
        createListeners(descriptionContentBinding.dirs4, 4)
        createListeners(descriptionContentBinding.nav5, 5)
        createListeners(descriptionContentBinding.size6, 6)
        createListeners(descriptionContentBinding.alarms7, 7)
        createListeners(descriptionContentBinding.mainTb8, 8)
        createListeners(descriptionContentBinding.sideTb9, 9)
        createListeners(descriptionContentBinding.moveMode10, 10)
        createListeners(descriptionContentBinding.delMode11, 11)
        createListeners(descriptionContentBinding.restMode12, 12)
        createListeners(descriptionContentBinding.archive13, 13)
        createListeners(descriptionContentBinding.convert14, 14)
        createListeners(descriptionContentBinding.send15, 15)
    }

    private fun closeDescriptionContent() {
        descriptionContentBinding.llDescriptionContent.isVisible = false
        descBackPressedCallback.isEnabled = false
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createListeners (view: View, position: Int) {
        view.setOnClickListener { v ->
            AppHelper.requestFocusInTouch(v)
            goToTab(position, true)
        }
        view.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
        }
    }

    private fun goToTab(position: Int, showAnimation: Boolean) {
        if (position in 1..15) {
            AppHelper.requestFocusInTouch(binding.fon)
            viewPager.setCurrentItem(position - 1, showAnimation)

        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(CURRENT_TAB, viewPager.currentItem)
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}