package com.a2t.myapplication.description.ui.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.a2t.myapplication.R
import com.a2t.myapplication.common.model.DLAnimator
import com.a2t.myapplication.databinding.ContextMenuMoveBinding
import com.a2t.myapplication.databinding.FragmentDesc10MoveModeBinding
import com.a2t.myapplication.databinding.ToolbarModesBinding
import com.a2t.myapplication.description.ui.DescriptionActivity

class MoveModeFragment10 : Fragment() {
    private var _binding: FragmentDesc10MoveModeBinding? = null
    private val binding get() = _binding!!
    private lateinit var modesBinding: ToolbarModesBinding
    private lateinit var contextMenu: ContextMenuMoveBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDesc10MoveModeBinding.inflate(layoutInflater)
        modesBinding = binding.tbModes
        contextMenu = binding.cmMove
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (requireActivity() as DescriptionActivity).currentScrollView = binding.scrollView
        // Панель режимов
        modesBinding.clModesToolbar.isVisible = true
        modesBinding.btnSelectAll.isVisible = false
        modesBinding.btnAction.isVisible = true
        modesBinding.btnAction.text = getString(R.string.insert)
        modesBinding.tvModeHint.isVisible = false
        modesBinding.ivBarModes3.isVisible = false
        modesBinding.ivBarModes2.setImageResource(R.drawable.ic_move_mode_2)
        modesBinding.ivBarModes1.setImageResource(R.drawable.ic_move_mode_1)
        modesBinding.ivBarModes1.startAnimation(DLAnimator().animationMoveMode)               // Анимация
        modesBinding.countRecords.isVisible = true
        modesBinding.countRecords.text = "8"

        // Контекстное меню
         contextMenu.llContextMenuMove.isVisible = true

        binding.tvText6.setOnClickListener {
            binding.tvText7.isVisible = true
            binding.tvText8.isVisible = true
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}