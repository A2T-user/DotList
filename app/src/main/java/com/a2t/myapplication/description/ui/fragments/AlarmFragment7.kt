package com.a2t.myapplication.description.ui.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.a2t.myapplication.databinding.FragmentDesc7AlarmBinding

class AlarmFragment7 : Fragment() {
    private var _binding: FragmentDesc7AlarmBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentDesc7AlarmBinding.inflate(layoutInflater)

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}