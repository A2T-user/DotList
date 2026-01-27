package com.a2t.myapplication.description.ui.fragments

import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.a2t.myapplication.R
import com.a2t.myapplication.common.utilities.DLAnimator
import com.a2t.myapplication.databinding.FragmentDesc2RecordsBinding
import com.a2t.myapplication.databinding.ItemMainBinding
import com.a2t.myapplication.description.ui.DescriptionActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RecordsFragment2 : Fragment() {
    private var _binding: FragmentDesc2RecordsBinding? = null
    private val binding get() = _binding!!
    private lateinit var recordBinding: ItemMainBinding
    private lateinit var dlAnimator: DLAnimator

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDesc2RecordsBinding.inflate(layoutInflater)
        recordBinding = binding.record
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (requireActivity() as DescriptionActivity).currentScrollView = binding.scrollView

        dlAnimator = DLAnimator()

        val date = DateFormat.format("d.M.yy", System.currentTimeMillis()).toString()
        // Макет строки
        recordBinding.ivBell.isVisible = true
        recordBinding.ivBell.setImageResource(R.drawable.ic_bell_white)
        recordBinding.aetRecord.isEnabled = true
        recordBinding.aetRecord.setTextColor(ContextCompat.getColor(requireContext(),R.color.blue_icon))
        recordBinding.aetRecord.setText(R.string.main_field)
        recordBinding.aetNote.isEnabled = true
        recordBinding.aetNote.setTextColor(ContextCompat.getColor(requireContext(),R.color.blue_icon))
        recordBinding.aetNote.setText(R.string.one)
        recordBinding.tvDateTime.text = date

        animationRecord(dlAnimator.animRecord)

        // Чекбокс
        binding.checkbox2.isChecked = true
        animationCheckBox()

        // Колокольчик
        binding.ivBellRed.startAnimation(dlAnimator.animBell)


    }

    private fun animationRecord(animRecord: Animation) {
        recordBinding.llForeground.startAnimation(animRecord)
        lifecycleScope.launch {
            delay(9000)
            animationRecord(animRecord)
        }
    }

    private fun animationCheckBox() {
        binding.checkbox2.isChecked = !binding.checkbox2.isChecked
        lifecycleScope.launch {
            delay(1000)
            animationCheckBox()
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}