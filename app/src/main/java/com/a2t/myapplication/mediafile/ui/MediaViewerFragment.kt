package com.a2t.myapplication.mediafile.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.a2t.myapplication.R
import com.a2t.myapplication.databinding.FragmentMediaViewerBinding
import java.io.File
import java.util.Locale
import com.a2t.myapplication.common.App
import com.a2t.myapplication.common.utilities.AppHelper
import com.a2t.myapplication.common.utilities.DLAnimator
import com.a2t.myapplication.main.ui.activity.MainActivity
import com.a2t.myapplication.common.utilities.MediaFormats
import com.a2t.myapplication.mediafile.presentation.MediaViewerViewModel
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
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
    private var isMenuOpen = false
    private var isEdit = false
    private lateinit var ma: MainActivity
    private lateinit var dlAnimator: DLAnimator
    private var menuJob = lifecycleScope.launch {}

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
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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

        binding.photoWindow.post {
            loadMedia(viewModel.mediaFileName)
        }
        binding.photoWindow.setOnTouchListener { _, _ ->
            AppHelper.requestFocusInTouch(binding.ivSend)
            false
        }

        binding.llTopbar.setOnClickListener {
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
        val message = "$viewModel.record - $viewModel.note"
        sendTextWithAttachedFile(requireContext(), message, viewModel.mediaFileName)
    }

    fun sendTextWithAttachedFile(context: Context, message: String, fileName: String) {
        val mediaType = getMediaDir(fileName)
        val targetFile = File(requireContext().filesDir, "mediafiles/$mediaType/$fileName")
        if (!targetFile.exists() || !targetFile.isFile) {
            Toast.makeText(context, "Файл не найден", Toast.LENGTH_SHORT).show()
            return
        }
        val contentUri: Uri = FileProvider.getUriForFile(
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
        val chooserIntent = Intent.createChooser(intent, "Отправить сообщение")
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(chooserIntent)
        } catch (_: Exception) {
            Toast.makeText(context, "Не удалось отправить сообщение", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openMenu(open: Boolean) {
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
        val mediaType = getMediaDir(fileName)

        val directory = File(requireContext().getExternalFilesDir(null), "mediafiles/$mediaType")
        val file = File(directory, fileName)

        if (!file.exists()) {
            Toast.makeText(requireContext(), "Файл не найден", Toast.LENGTH_SHORT).show()
            return
        }

        when (mediaType) {
            "image" -> {
                binding.photoWindow.isVisible = true
                try {
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    if (bitmap == null || bitmap.isRecycled) {
                        Toast.makeText(requireContext(), "Ошибка загрузки изображения", Toast.LENGTH_SHORT).show()
                        return
                    }
                    val rotatedBitmap = rotateBitmapAccordingToExif(file, bitmap)
                    binding.photoWindow.setImage(ImageSource.bitmap(rotatedBitmap))
                    binding.photoWindow.setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CENTER_INSIDE)
                    binding.photoWindow.setOrientation(SubsamplingScaleImageView.ORIENTATION_USE_EXIF)
                    binding.photoWindow.setDoubleTapZoomScale(2f)
                    binding.photoWindow.setPanLimit(SubsamplingScaleImageView.PAN_LIMIT_INSIDE)
                } catch (_: Exception) {
                    Toast.makeText(requireContext(), "Ошибка загрузки изображения", Toast.LENGTH_SHORT).show()
                }
            }
            else -> {
                Toast.makeText(requireContext(), "Неподдерживаемый формат", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getMediaDir(fileName: String): String? {
        val ext = fileName.substringAfterLast(".", "").lowercase(Locale.getDefault())
        return when (ext) {
            in MediaFormats.imageExtensions -> "image"
            in MediaFormats.videoExtensions -> "video"
            else -> null
        }
    }

    // Определяет в какой ориентации сделано фото и поворачивает Bitmap в ту же ориентацию
    private fun rotateBitmapAccordingToExif(file: File, bitmap: Bitmap): Bitmap {
        return try {
            val exif = ExifInterface(file.absolutePath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

            val rotationDegrees = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }

            if (rotationDegrees != 0) {
                val matrix = Matrix()
                matrix.postRotate(rotationDegrees.toFloat())
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } else {
                bitmap
            }
        } catch (_: Exception) {
            bitmap
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
        val imm = checkNotNull(App.appContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
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