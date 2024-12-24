package com.a2t.myapplication.description.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.ScrollView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.viewpager2.widget.ViewPager2
import com.a2t.myapplication.databinding.ActivityDescriptionBinding
import com.a2t.myapplication.databinding.DescriptionContentBinding

const val CURRENT_TAB = "current_tab"
const val STEP_SCROLL = 20

class DescriptionActivity : AppCompatActivity() {
    private lateinit var mainBackPressedCallback: OnBackPressedCallback
    private var _binding: ActivityDescriptionBinding? = null
    private val binding get() = _binding!!
    private lateinit var descriptionContentBinding: DescriptionContentBinding
    private lateinit var viewPager: ViewPager2
    lateinit var currentScrollView: ScrollView


    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityDescriptionBinding.inflate(layoutInflater)
        descriptionContentBinding = binding.descContent
        setContentView(binding.root)

        // $$$$$$$$$$$$$$$$$$$$$$   Реакция на нажатие системной кнопки BACK   $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$
        mainBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                requestFocusInTouch(descriptionContentBinding.descGeneral1)
            }
        }
        mainBackPressedCallback.isEnabled = false
        onBackPressedDispatcher.addCallback(this, mainBackPressedCallback)

        viewPager = binding.pager
        val adapter = PagerAdapter(this)
        viewPager.adapter = adapter

        val currentTab = savedInstanceState?.getInt(CURRENT_TAB, 0) // Значение по умолчанию - 0
            ?: intent.getIntExtra(CURRENT_TAB, 0)
        goToTab(currentTab, false)

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

        binding.goToUp.setOnTouchListener { v, event ->
            currentScrollView.smoothScrollTo(0, currentScrollView.scrollY - STEP_SCROLL)
            true
        }

        binding.goToDown.setOnTouchListener { v, event ->
            currentScrollView.smoothScrollTo(0, currentScrollView.scrollY + STEP_SCROLL)
            true
        }

        binding.goToDescContent.setOnClickListener {
            descriptionContentBinding.llDescriptionContent.isVisible = true
            descriptionContentBinding.llDescriptionContent.requestFocus()
            mainBackPressedCallback.isEnabled = true
        }

        descriptionContentBinding.llDescriptionContent.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) descriptionContentBinding.llDescriptionContent.isVisible = false
            mainBackPressedCallback.isEnabled = false
        }

        binding.fon.setOnTouchListener { v, _ ->
            requestFocusInTouch(v)
            true
        }

        descriptionContentBinding.descGeneral1.setOnClickListener { v ->
            requestFocusInTouch(v)
            goToTab(1, true)
        }
        descriptionContentBinding.records2.setOnClickListener { v ->
            requestFocusInTouch(v)
            goToTab(2, true)
        }
        descriptionContentBinding.lines3.setOnClickListener { v ->
            requestFocusInTouch(v)
            goToTab(3, true)
        }
        descriptionContentBinding.dirs4.setOnClickListener { v ->
            requestFocusInTouch(v)
            goToTab(4, true)
        }
        descriptionContentBinding.nav5.setOnClickListener { v ->
            requestFocusInTouch(v)
            goToTab(5, true)
        }
        descriptionContentBinding.size6.setOnClickListener { v ->
            requestFocusInTouch(v)
            goToTab(6, true)
        }
        descriptionContentBinding.alarms7.setOnClickListener { v ->
            requestFocusInTouch(v)
            goToTab(7, true)
        }
        descriptionContentBinding.mainTb8.setOnClickListener { v ->
            requestFocusInTouch(v)
            goToTab(8, true)
        }
        descriptionContentBinding.sideTb9.setOnClickListener { v ->
            requestFocusInTouch(v)
            goToTab(9, true)
        }
        descriptionContentBinding.moveMode10.setOnClickListener { v ->
            requestFocusInTouch(v)
            goToTab(10, true)
        }
        descriptionContentBinding.delMode11.setOnClickListener { v ->
            requestFocusInTouch(v)
            goToTab(11, true)
        }
        descriptionContentBinding.restMode12.setOnClickListener { v ->
            requestFocusInTouch(v)
            goToTab(12, true)
        }
        descriptionContentBinding.archive13.setOnClickListener { v ->
            requestFocusInTouch(v)
            goToTab(13, true)
        }
        descriptionContentBinding.convert14.setOnClickListener { v ->
            requestFocusInTouch(v)
            goToTab(14, true)
        }
        descriptionContentBinding.send15.setOnClickListener { v ->
            requestFocusInTouch(v)
            goToTab(15, true)
        }

    }

    private fun goToTab(position: Int, showAnimation: Boolean) {
        if (position in 1..15) {
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