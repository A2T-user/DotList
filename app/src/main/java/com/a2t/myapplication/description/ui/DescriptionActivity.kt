package com.a2t.myapplication.description.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
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
                requestFocusInTouch(descriptionContentBinding.descGeneral1)
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
            override fun onSwipeLeft() {
                descriptionContentBinding.llDescriptionContent.isVisible = false
                descBackPressedCallback.isEnabled = false
            }

            override fun onSwipeRight() {}
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
            if (!hasFocus) descriptionContentBinding.llDescriptionContent.isVisible = false
            descBackPressedCallback.isEnabled = false
        }

        descriptionContentBinding.llDescriptionContent.setOnTouchListener { _, event ->
            Log.e("МОЁ", "Touch")
            gestureDetector.onTouchEvent(event)
            true
        }

        binding.fon.setOnTouchListener { v, _ ->
            requestFocusInTouch(v)
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

    @SuppressLint("ClickableViewAccessibility")
    private fun createListeners (view: View, position: Int) {
        view.setOnClickListener { v ->
            requestFocusInTouch(v)
            goToTab(position, true)
        }
        view.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
        }
    }

    private fun goToTab(position: Int, showAnimation: Boolean) {
        if (position in 1..15) {
            requestFocusInTouch(binding.fon)
            viewPager.setCurrentItem(position - 1, showAnimation)

        }
    }

    private fun requestFocusInTouch(view: View) {
        view.isFocusableInTouchMode = true
        view.requestFocus()
        view.isFocusableInTouchMode = false
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