package com.a2t.myapplication.description.ui.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.a2t.myapplication.R
import com.a2t.myapplication.common.utilities.DLAnimator
import com.a2t.myapplication.databinding.FragmentDesc11DelModeBinding
import com.a2t.myapplication.databinding.ToolbarModesBinding
import com.a2t.myapplication.description.ui.DescriptionActivity

class DelModeFragment11 : Fragment() {
    private var _binding: FragmentDesc11DelModeBinding? = null
    private val binding get() = _binding!!
    private lateinit var modesBinding: ToolbarModesBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentDesc11DelModeBinding.inflate(layoutInflater)
        modesBinding = binding.tbModes
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (requireActivity() as DescriptionActivity).currentScrollView = binding.scrollView
        // Панель режимов
        modesBinding.clModesToolbar.isVisible = true
        modesBinding.btnSelectAll.isVisible = true
        modesBinding.btnAction.isVisible = true
        modesBinding.btnAction.text = getString(R.string.delete)
        modesBinding.tvModeHint.isVisible = true
        modesBinding.ivBarModes3.isVisible = false
        modesBinding.ivBarModes2.setImageResource(R.drawable.ic_basket)
        modesBinding.ivBarModes1.setImageResource(R.drawable.ic_arrow_red)
        modesBinding.ivBarModes1.startAnimation(DLAnimator().animationDeleteMode)             // Анимация

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}