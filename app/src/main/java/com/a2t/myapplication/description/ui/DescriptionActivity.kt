package com.a2t.myapplication.description.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.a2t.myapplication.databinding.ActivityDescriptionBinding

class DescriptionActivity : AppCompatActivity() {
    private var _binding: ActivityDescriptionBinding? = null
    private val binding get() = _binding!!


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityDescriptionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adapter = PagerAdapter(this)
        binding.pager.adapter = adapter

    }

    override fun onDestroy() {
        super.onDestroy()

        _binding = null // Очищаем привязку, чтобы избежать утечек памяти
    }
}