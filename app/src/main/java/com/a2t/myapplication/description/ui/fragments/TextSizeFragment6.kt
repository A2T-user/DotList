package com.a2t.myapplication.description.ui.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.a2t.myapplication.databinding.FragmentDesc6TextSizeBinding

class TextSizeFragment6 : Fragment() {
    private var _binding: FragmentDesc6TextSizeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentDesc6TextSizeBinding.inflate(layoutInflater)

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}