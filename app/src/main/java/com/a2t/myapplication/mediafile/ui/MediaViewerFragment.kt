package com.a2t.myapplication.mediafile.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.a2t.myapplication.R
import com.a2t.myapplication.databinding.FragmentMediaViewerBinding
import java.io.File
import com.a2t.myapplication.common.App
import com.a2t.myapplication.common.utilities.AppHelper
import com.a2t.myapplication.common.utilities.DLAnimator
import com.a2t.myapplication.main.ui.activity.MainActivity
import com.a2t.myapplication.common.utilities.MediaFormats
import com.a2t.myapplication.mediafile.data.dto.MediaFileType
import com.a2t.myapplication.mediafile.presentation.MediaViewerViewModel
import com.a2t.myapplication.mediafile.ui.util.DownloaderMediaFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.getValue

class MediaViewerFragment : Fragment() {
    private val viewModel: MediaViewerViewModel by viewModel()
    private var _binding: FragmentMediaViewerBinding? = null
    private val binding get() = _binding!!
    private lateinit var context: Context
    private var isMenuOpen = false
    private var isEdit = false
    private lateinit var ma: MainActivity
    private lateinit var dlAnimator: DLAnimator
    private var menuJob = lifecycleScope.launch {}
    private lateinit var listPreviewWindows: List<View>

    companion object {
        const val RECORD_ID = "record_id"
        const val RECORD = "record"
        const val NOTE = "note"
        const val FILE_NAME = "fileName"

        fun newInstance(id: Long, record: String, note: String, fileName: String): MediaViewerFragment {
            return MediaViewerFragment().apply {
                arguments = Bundle().apply {
                    putLong(RECORD_ID, id)
                    putString(RECORD, record)
                    putString(NOTE, note)
                    putString(FILE_NAME, fileName)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            viewModel.id = it.getLong(RECORD_ID)
            viewModel.record = it.getString(RECORD, "")
            viewModel.note = it.getString(NOTE, "")
            viewModel.mediaFileName = it.getString(FILE_NAME, "")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMediaViewerBinding.inflate(layoutInflater)
        listPreviewWindows = listOf(binding.imageWindow)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        context = requireContext()
        ma = requireActivity() as MainActivity
        dlAnimator = DLAnimator()

        // Установка размера шрифта
        App.getTextSizeLiveData().observe(viewLifecycleOwner) { size ->
            binding.aetRecord.textSize = size
            binding.aetNote.textSize = 0.75f * size
        }

        binding.aetRecord.setText(viewModel.record)
        binding.aetNote.setText(viewModel.note)
        binding.aetNote.isVisible = viewModel.note.isNotEmpty()

        binding.imageWindow.post {
            loadMedia(viewModel.mediaFileName)
        }
        binding.imageWindow.setOnTouchListener { _, _ ->
            AppHelper.requestFocusInTouch(binding.ivSend)
            false
        }

        binding.llTopbar.setOnClickListener {
            startEditMode()
        }

        binding.ivEdit.setOnClickListener {
            startEditMode()
        }

        binding.ivMediaMinus .setOnClickListener {
            unAttachMediaFile()
        }

        binding.ivSend.setOnClickListener {v ->
            AppHelper.requestFocusInTouch(v)
            if (isMenuOpen){
                sendFile()
            } else {
                openMenu(true)
            }
        }
        // РЕАКЦИЯ ТЕКСТОВЫХ ПОЛЕЙ*****************************************************************************************************************
        // Отклик поля RECORD на нажатие клавиши ОК клавиатуры
        binding.aetRecord.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE || (event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER)) {
                if (binding.aetRecord.getText().toString().isEmpty()) {          // Если поле RECORD пустое
                    endEditMode()                        // Завершения редактирования
                } else {                                                        // Если поле RECORD не пустое
                    binding.aetNote.requestFocus()                               // переход в поле NOTE
                }
            }
            true
        }
        // Отклик поля NOTE на нажатие клавиши ОК клавиатуры
        binding.aetNote.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE
                || (event.action == KeyEvent.ACTION_DOWN
                        && event.keyCode == KeyEvent.KEYCODE_ENTER)) {
                endEditMode()                         // Завершения редактирования
            }
            true
        }
        // Отклик поля RECORD на потерю фокуса - завершения редактирования
        binding.aetRecord.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus && !binding.aetNote.hasFocus() && isEdit)     // Если фокус перешел не в поле NOTE,
                endEditMode() //                     // завершить редактирование
        }
        // Отклик поля NOTE на получение фокуса - проверка пуст.строка/потерю - завершения редактирования
        binding.aetNote.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                if (binding.aetRecord.getText().toString().isEmpty()) {       // Если строка RECORD пустая,
                    binding.aetRecord.requestFocus()                         // перевести фокус в поле RECORD
                }
            } else if (!binding.aetRecord.hasFocus() && isEdit) {           // Если фокус перешел не в поле RECORD
                endEditMode()                    // Завершить редактирование
            }
        }
    }

    private fun unAttachMediaFile() {
        // Обновляем строку БД
        viewModel.updateMediaFile(viewModel.id, null)
        // Обновляем данные в MainActivity
        updatingMediaFileInMainActivity()
        parentFragmentManager.beginTransaction().remove(this@MediaViewerFragment).commitAllowingStateLoss() // Закрытие фрагмента
    }
    private fun updatingMediaFileInMainActivity() {
        // Обновляем данные в MainActivity
        val records = ma.adapter.records
        val position = records.indexOfFirst { it.id == viewModel.id }
        // Обновление данных в массиве
        val item = records[position]
        item.mediaFile = null
        // Обновление холдера
        ma.adapter.notifyItemChanged(position)
    }

    private fun sendFile() {
        val message = if (viewModel.note.isEmpty()) viewModel.record else "${viewModel.record} - ${viewModel.note}"
        sendTextWithAttachedFile(message, viewModel.mediaFileName)
    }

    fun sendTextWithAttachedFile(message: String, fileName: String) {
        val mediaFileType = getMediaFileType(fileName)
        if (mediaFileType == null) {
            AppHelper.errorDialog(ma, getString(R.string.unknown_file_type))
            return
        }
        val mediaType = mediaFileType.dir
        val targetFile = File(context.getExternalFilesDir(null), "mediafiles/$mediaType/$fileName")
        if (!targetFile.exists() || !targetFile.isFile) {
            AppHelper.errorDialog(ma, getString(R.string.file_not_found))
            return
        }
        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            targetFile
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
        }
        val chooserIntent = Intent.createChooser(intent, context.getString(R.string.send_message))
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(chooserIntent)
        } catch (_: Exception) {
            AppHelper.errorDialog(ma, getString(R.string.send_message_error))
        }
    }

    private fun openMenu(open: Boolean) {
        binding.ivEdit.isVisible = open
        binding.ivMediaMinus.isVisible = open
        val title = if (open) R.drawable.ic_send_white else R.drawable.ic_menu_white
        dlAnimator.flipPicture(binding.ivSend, title)
        isMenuOpen = open
        if (open) {
            menuJob = lifecycleScope.launch {
                menuJob.cancel()
                delay(3000)
                openMenu(false)
            }
        }
    }

    private fun loadMedia(fileName: String) {
        val mediaFileType = getMediaFileType(fileName)
        if (mediaFileType != null) {
            val file = createFile(fileName, mediaFileType)
            if (file.exists()) {
                val downloaderMediaFile =
                    DownloaderMediaFile(context, listPreviewWindows, binding.ivPlaceholder)
                downloaderMediaFile.loadMedia(mediaFileType, null, file)
            } else {
                binding.ivPlaceholder.isVisible = true
            }
        }
    }

    private fun createFile(fileName: String, mediaFileType: MediaFileType): File {
        val directory =
            File(context.getExternalFilesDir(null), "mediafiles/${mediaFileType.dir}")
        return File(directory, fileName)
    }

    private fun getMediaFileType(fileName: String): MediaFileType? {
        val ext = fileName.substringAfterLast(".", "").lowercase()
        return when (ext) {
            in MediaFormats.imageExtensions -> MediaFileType.IMAGE
            in MediaFormats.videoExtensions -> MediaFileType.VIDEO
            else -> null
        }
    }

    // Старт режима редактирования
    private fun startEditMode() {
        if (isMenuOpen) openMenu(false)
        isEdit = true
        binding.aetNote.isVisible = true

        // Разблокируем поля RECORD и PRIM и переводим фокус в поле RECORD
        enableEditText(binding.aetRecord, true)
        enableEditText(binding.aetNote, true)
        binding.aetRecord.requestFocus()
        binding.aetRecord.setSelection(binding.aetRecord.getText()!!.length) // Курсор в конец строки
        showKeyboard(binding.aetRecord, true) // вывести клавиатуру

    }
    private fun endEditMode() {
        if (isEdit) {                  // Если включен режим редактирования
            isEdit = false             // Выключаем режим редактирования
            // Отбрасываем начальные и конечные пробелы в полях RECORD и NOTE
            var str: String = binding.aetRecord.text.toString().trim()
            binding.aetRecord.setText(str)
            str = binding.aetNote.getText().toString().trim()
            binding.aetNote.setText(str)
            if (binding.aetRecord.getText().toString().isEmpty()) {  // Если aetRecord пустое,
                binding.aetRecord.setText(viewModel.record)          // вернуть старое значение строки из массива
            } else if (binding.aetRecord.getText().toString() != viewModel.record
                || binding.aetNote.getText().toString() != viewModel.record) { // Новые значения не равны старым
                // Обновляем параметры
                viewModel.record = binding.aetRecord.getText().toString()
                viewModel.note = binding.aetNote.getText().toString()
                viewModel.updateRecordAndNote(viewModel.id, viewModel.record, viewModel.note)  // Сохранение в БД
                updatatingRecordAndNoteInMainActivity ()
            }
            binding.aetNote.isVisible = !viewModel.note.isEmpty() // Вывод поля  NOTE
        }
        showKeyboard(binding.aetRecord, false) // Убрать клавиатуру
        enableEditText(binding.aetRecord, false) // Закрываем поле RECORD
        enableEditText(binding.aetNote, false)   // Закрываем поле NOTE
    }
    private fun updatatingRecordAndNoteInMainActivity () {
        // Обновляем данные в MainActivity
        val records = ma.adapter.records
        val position = records.indexOfFirst { it.id == viewModel.id }
        // Обновление данных в массиве
        val item = records[position]
        item.record = viewModel.record
        item.note = viewModel.note
        // Обновление холдера
        ma.adapter.notifyItemChanged(position)
    }
    // Вывести/убрать клавиатуру
    private fun showKeyboard(et: EditText, show: Boolean) {
        val imm = checkNotNull(context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
        CoroutineScope(Dispatchers.Main).launch {
            delay(10)
            if (show) {      // Вывести клавиатуру
                imm.showSoftInput(et, InputMethodManager.SHOW_IMPLICIT)
            } else {            // Убрать клавиатуру
                imm.hideSoftInputFromWindow(et.windowToken, 0)
            }
        }
    }
    // Открытие/закрытие доступа к полю EditText
    private fun enableEditText(et: EditText, enable: Boolean) {
        et.isEnabled = enable
        et.isClickable = enable
        et.isLongClickable = enable
    }

    override fun onStart() {
        super.onStart()
        ma.mainBackPressedCallback.isEnabled = false
    }

    override fun onStop() {
        super.onStop()
        ma.mainBackPressedCallback.isEnabled = true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        menuJob.cancel()
        _binding = null
    }
}