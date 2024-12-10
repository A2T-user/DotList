package com.a2t.myapplication.text.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.a2t.myapplication.App
import com.a2t.myapplication.R
import com.a2t.myapplication.databinding.FragmentTextBinding
import com.a2t.myapplication.main.domain.model.ListRecord
import com.a2t.myapplication.root.presentation.SharedViewModel
import com.a2t.myapplication.root.presentation.model.TextFragmentMode
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class TextFragment : Fragment() {
    private val sharedViewModel: SharedViewModel by activityViewModel()
    private lateinit var binding: FragmentTextBinding
    private var mode: TextFragmentMode? = null
    private var idCurrentDir: Long = 0
    private val records = ArrayList<ListRecord>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mode = sharedViewModel.textFragmentMode
        idCurrentDir = sharedViewModel.idCurrentDir
        records.clear()
        records.addAll(sharedViewModel.mainRecords)
        binding = FragmentTextBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val textSize = App.appSettings.textSize
        binding.etText.textSize = textSize
        binding.btnAction.textSize = textSize
        binding.btnCancel.textSize = textSize
        binding.tvHint.textSize = 0.6f * textSize
        if (mode != null) {
            if (mode == TextFragmentMode.CONVERT) {
                showConvertMode()
            } else {
                showSendMode()
            }
        } else {
            Toast.makeText(requireContext(), getString(R.string.something_went_wrong), Toast.LENGTH_SHORT).show()
            requireActivity().supportFragmentManager.popBackStack()
        }

        // Кнопка Отмена
        binding.btnCancel.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        // Кнопка Action
        binding.btnAction.setOnClickListener {
            if (mode == TextFragmentMode.CONVERT) {

            } else {
                sendList()
            }



        }
    }

    private fun showConvertMode() {
        binding.tvTitle.text = getString(R.string.title_conversion)
        val newDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_convert_text)
        binding.tvTitle.setCompoundDrawablesWithIntrinsicBounds(newDrawable, null, null, null)
        binding.tvHint.isVisible = true
        binding.btnAction.text = getString(R.string.btn_convert_text)
    }

    private fun showSendMode() {
        binding.tvTitle.text = getString(R.string.title_send)
        val newDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_send)
        binding.tvTitle.setCompoundDrawablesWithIntrinsicBounds(newDrawable, null, null, null)
        binding.tvHint.isVisible = false
        binding.btnAction.text = getString(R.string.btn_send_text)
        binding.etText.setText(convertListToString())
    }

    private fun convertListToString(): String {
        var string = ""
        records.forEachIndexed { index, record ->
            if (!record.isNew) {
                string = string + (index + 1).toString() + ". " + record.record
                if (record.note.isNotEmpty()) string = string + " - " + record.note
                string += "\n"
            }
        }
        return string
    }

    private fun sendList() {
        val string = binding.etText.text.toString()
        if (string.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.send_text_empty), Toast.LENGTH_SHORT).show()
        } else {
            val intent = Intent(Intent.ACTION_SEND)
            intent.setType("text/plain")
            intent.putExtra(Intent.EXTRA_TEXT, string)
            val chooserIntent = Intent.createChooser(intent, getString(R.string.mail_service))
            startActivity(chooserIntent)
            requireActivity().supportFragmentManager.popBackStack()
        }
    }
}