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
import com.a2t.myapplication.root.domain.model.ListRecord
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

        App.getTextSizeLiveData().observe(viewLifecycleOwner) { size ->
            binding.etText.textSize = size
            binding.btnAction.textSize = size
            binding.btnCancel.textSize = size
            binding.tvHint.textSize = 0.6f * size
        }

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
                val convertText = binding.etText.text.trim { it <= ' ' }
                if (convertText.isNotEmpty()) {
                    val convertRecords = convertStringToList()
                    if (convertRecords.isNotEmpty()) {
                        sharedViewModel.insertRecords(convertRecords)
                        records.addAll(convertRecords)
                        sharedViewModel.mainRecords.clear()
                        sharedViewModel.mainRecords.addAll(records)
                        requireActivity().supportFragmentManager.popBackStack()
                    }
                } else {
                    Toast.makeText(requireContext(), getString(R.string.convert_text_empty), Toast.LENGTH_SHORT).show()
                }
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

    private fun convertStringToList(): List<ListRecord> {
        val convertRecords = ArrayList<ListRecord>()
        val string = binding.etText.text.toString()
        val maxNpp: Int = records.maxOfOrNull { it.npp } ?: 0
        if (string.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.convert_text_hint), Toast.LENGTH_SHORT).show()
        } else {
            val stringList: Array<String> =
                string.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray() // Разбиваем текст на строки
            // Обрабатываем каждую троку
            stringList.forEachIndexed { index, st ->
                var str = st
                val record: StringBuilder
                var note = ""
                str = str.trim { it <= ' ' } // Отбрасывание начальных и конечных пробелов
                if (str.isNotEmpty()) {    // Отбрасывание пустых строк
                    // Определение наличия порядкового номера строки
                    var s = str.split("\\. ".toRegex(), limit = 2).toTypedArray()
                    // Отбрасывание порядкового номера
                    if (s.size > 1) {
                        if (isTheStringANumber(s[0])) str = s[1]
                    }
                    // Разбиение на record и prim
                    s = str.split(" - ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    val l = s.size
                    record = StringBuilder(s[0])
                    if (l > 1) {
                        note = s[l - 1]
                        for (i in 1 until l - 1) record.append(" - ").append(s[i])
                    }
                    // Добавление нового элемента в convertRecords
                    convertRecords.add(getNewItem(record.toString(), note, maxNpp, index))
                }

            }
        }
        return convertRecords
    }

    private fun getNewItem(record: String, note: String, maxNpp: Int, index: Int): ListRecord {
        return ListRecord(
            0L,
            idCurrentDir,
            isDir = false,
            maxNpp + 1 + index,
            isChecked = false,
            record,
            note,
            0,
            0,
            0,
            System.currentTimeMillis(),
            null,
            null,
            null,
            isArchive = false,
            isDelete = false,
            isFull = false,
            isAllCheck = false,
            isNew = false,
            startEdit = false,
            isEdit = false
        )
    }

    // Определяет может ли быть преобразована строка в INT
    private fun isTheStringANumber(str: String): Boolean {
        var result = true
        try {
            str.toInt() // Преобразуем строку в число
        } catch (e: NumberFormatException) {
            result = false   // Если строка не переобразуеся в число
        }
        return result
    }
}