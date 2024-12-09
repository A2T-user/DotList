package com.a2t.myapplication.text.ui


import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.a2t.myapplication.R
import com.a2t.myapplication.databinding.FragmentTextBinding
import com.a2t.myapplication.main.presentation.MainViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class TextFragment : Fragment() {
    private val mainViewModel by viewModel<MainViewModel>()
    private lateinit var binding: FragmentTextBinding
    private var mode: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTextBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mode = mainViewModel.openingModeTextFragment
        Log.e("МОЁ", "mode = " + mode)
        if (mode != null) {

            if (mode.equals("CONVERT")) showConvertMode() else showSendMode()
        }

    }

    private fun showConvertMode() {
        binding.tvTitle.text = getString(R.string.title_conversion)
        val newDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_convert_text)
        binding.tvTitle.setCompoundDrawablesWithIntrinsicBounds(newDrawable, null, null, null)
        binding.tvHint.isVisible = true
        binding.btnCancel.text = getString(R.string.title_conversion)
    }

    private fun showSendMode() {
        binding.tvTitle.text = getString(R.string.title_send)
        val newDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_send)
        binding.tvTitle.setCompoundDrawablesWithIntrinsicBounds(newDrawable, null, null, null)
        binding.tvHint.isVisible = false
        binding.btnCancel.text = getString(R.string.btn_send_text)
    }
}