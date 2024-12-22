package com.a2t.myapplication.description.ui.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.a2t.myapplication.databinding.FragmentDesc5NavBinding
import com.a2t.myapplication.databinding.ToolbarSmallBinding

class NavFragment5 : Fragment() {
    private var _binding: FragmentDesc5NavBinding? = null
    private val binding get() = _binding!!
    private lateinit var smallBarBinding: ToolbarSmallBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDesc5NavBinding.inflate(layoutInflater)
        smallBarBinding = binding.tbSmall
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        smallBarBinding.tvSumLine.text = "4"
        smallBarBinding.tvSumDir.text = "1"
        smallBarBinding.tvSumSum.text = "5"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}