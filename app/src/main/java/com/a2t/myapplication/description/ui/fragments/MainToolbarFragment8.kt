package com.a2t.myapplication.description.ui.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.a2t.myapplication.R
import com.a2t.myapplication.databinding.FragmentDesc8MainToolbarBinding
import com.a2t.myapplication.databinding.ToolbarTopBinding

class MainToolbarFragment8 : Fragment() {
    private var _binding: FragmentDesc8MainToolbarBinding? = null
    private val binding get() = _binding!!
    private lateinit var toolbarBinding: ToolbarTopBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentDesc8MainToolbarBinding.inflate(layoutInflater)
        toolbarBinding = binding.tbTop

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbarBinding.pathDir.text = getString(R.string.current_dir_name)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}