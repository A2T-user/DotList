package com.a2t.myapplication.description.ui.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.a2t.myapplication.databinding.FragmentDesc9SideToolbarBinding
import com.a2t.myapplication.description.ui.DescriptionActivity

class SideToolbarFragment9 : Fragment() {
    private var _binding: FragmentDesc9SideToolbarBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentDesc9SideToolbarBinding.inflate(layoutInflater)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (requireActivity() as DescriptionActivity).currentScrollView = binding.scrollView
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}