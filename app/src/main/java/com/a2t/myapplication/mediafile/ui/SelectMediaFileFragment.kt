package com.a2t.myapplication.mediafile.ui


import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.TypedValue
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.a2t.myapplication.R
import com.a2t.myapplication.databinding.FragmentSelectMediaFileBinding
import com.a2t.myapplication.main.ui.activity.MainActivity
import com.a2t.myapplication.main.ui.activity.recycler.MyScrollListener
import com.a2t.myapplication.main.ui.activity.recycler.OnScrollStateChangedListener
import com.a2t.myapplication.main.ui.activity.recycler.model.ScrollState
import com.a2t.myapplication.mediafile.data.dto.DirType
import com.a2t.myapplication.mediafile.data.dto.ErrCode
import com.a2t.myapplication.mediafile.data.dto.MediaFileType
import com.a2t.myapplication.mediafile.data.dto.Response
import com.a2t.myapplication.mediafile.domaim.model.MediaItem
import com.a2t.myapplication.mediafile.presentation.MediaFileViewModel
import com.a2t.myapplication.mediafile.ui.recycler.MediaFileAdapter
import com.a2t.myapplication.mediafile.ui.recycler.MediaFileAdapterCallback
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import androidx.exifinterface.media.ExifInterface
import com.a2t.myapplication.mediafile.presentation.model.MediaFileFilter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.getValue

class SelectMediaFileFragment : Fragment(), MediaFileAdapterCallback, OnScrollStateChangedListener {
    private val mediaFileViewModel: MediaFileViewModel by viewModel()
    private var _binding: FragmentSelectMediaFileBinding? = null
    private val binding get() = _binding!!
    private lateinit var context: Context
    private lateinit var ma: MainActivity
    private var idString: Long? = null
    private lateinit var recycler: RecyclerView
    lateinit var adapter: MediaFileAdapter
    private lateinit var photoLauncher: ActivityResultLauncher<Intent>
    private var scrollJob = lifecycleScope.launch {}
    private var loadMediaJob = lifecycleScope.launch {}
    private var scrollState = ScrollState.STOPPED
    private var isPreviewContainerBig = false
    private var photoUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        idString = arguments?.getLong(ARG_ID)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        photoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
            photoUri?.let { uri ->
                if (checkUriReady(uri)) {
                    mediaFileViewModel.addPhotoToExternalAppStorage(photoUri!!)
                }
            }
        }
        _binding = FragmentSelectMediaFileBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        context = requireContext()
        ma = requireActivity() as MainActivity
        adapter = MediaFileAdapter(this)
        recycler = binding.recycler
        recycler.adapter = adapter
        recycler.layoutManager = GridLayoutManager(requireContext(), 3)
        recycler.itemAnimator = DefaultItemAnimator()
        recycler.scheduleLayoutAnimation()
        recycler.invalidate()
        requestMediaPermissions()

        // ПРОКРУТКА
        recycler.addOnScrollListener(MyScrollListener(this))

        binding.ivBtnScroll.setOnClickListener {
            when(scrollState) {
                ScrollState.DOWN -> recycler.scrollToPosition(adapter.itemCount - 1)
                ScrollState.UP -> recycler.scrollToPosition(0)
                else -> {}
            }
        }
        // Следим за состоянием Загрузка - выводим/убираем прогрессбар
        mediaFileViewModel.getIsLoadingLiveData().observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.isVisible = isLoading
        }
        // Следим за обновлением массива данных рециклера
        mediaFileViewModel.getItemListLiveData().observe(viewLifecycleOwner) { newList ->
            adapter.itemList.clear()
            adapter.itemList.addAll(newList)
            adapter.notifyDataSetChanged()
        }
        // Следим за изменением параметров фильтрации - папки и типа файлов
        mediaFileViewModel.getFilterLiveData().observe(viewLifecycleOwner) { filter ->
            val view = when (filter.dir) {
                DirType.GALLERY -> binding.tvGallery
                DirType.APP -> binding.tvApp
            }
            selectedDir(view)
            mediaFileViewModel.filterListItems()
        }

        // Следим за сохранением файла во ВНУТРЕННЕМ хранилище
        mediaFileViewModel.getResponseCopyToPrivateStorageLiveData().observe(viewLifecycleOwner) { response ->
            when(response) {
                is Response.Success -> {
                    val fileName = response.fileName
                    // Обновляем строку БД
                    mediaFileViewModel.updateMediaFile(idString!!, fileName)
                    // Обновляем данные в MainActivity
                    updatingMainActivity(fileName)
                    parentFragmentManager.beginTransaction().remove(this@SelectMediaFileFragment).commitAllowingStateLoss() // Закрытие фрагмента
                }
                is Response.FileFromExternalAppStorage -> {
                    val fileName = response.fileName
                    // Обновляем строку БД
                    mediaFileViewModel.updateMediaFile(idString!!, fileName)
                    // Обновляем данные в MainActivity
                    updatingMainActivity(fileName)
                    parentFragmentManager.beginTransaction().remove(this@SelectMediaFileFragment).commitAllowingStateLoss() // Закрытие фрагмента
                }
                is Response.FileExists -> {
                    @SuppressLint("InflateParams")
                    val dialogView =
                        LayoutInflater.from(context).inflate(R.layout.dialog_title_attention, null)
                    MaterialAlertDialogBuilder(context)
                        .setCustomTitle(dialogView)
                        .setMessage(getString(R.string.dialog_hint))
                        .setNeutralButton(getString(R.string.existing_copy)) { _, _ ->
                            mediaFileViewModel.filterExistingFiles(response.originalName, response.mediaFileType)
                            selectedDir(binding.tvApp)
                        }
                        .setPositiveButton(getString(R.string.new_copy)) { _, _ ->
                            mediaFileViewModel.saveImageToExternalAppStorage(
                                response.uri,
                                response.mediaFileType,
                                DirType.GALLERY,
                                true
                                )
                        }
                        .show()
                }
                is Response.Error -> {
                    parentFragmentManager.beginTransaction().remove(this@SelectMediaFileFragment).commitAllowingStateLoss() // Закрытие фрагмента
                    val res = when(response.errCode) {
                        ErrCode.OUT_OF_MEMORY -> R.string.out_of_memory
                        ErrCode.COPY_ERROR -> R.string.file_not_saved
                        ErrCode.UNEXPECTED_ERROR -> R.string.file_not_saved
                    }
                    Toast.makeText(requireContext(), res, Toast.LENGTH_SHORT).show()
                }
            }
        }
        // Следим за сохранением файла в ОБЩЕМ хранилище
        mediaFileViewModel.getResponseCopyToPublicStorageLiveData().observe(viewLifecycleOwner) { response ->
            when(response) {
                is Response.Success -> {
                    // добавляем item в baseListItem
                    val dateFormat = SimpleDateFormat("dd.MM.yy", Locale.getDefault())
                    val currentDate = dateFormat.format(Date())
                    val item = MediaItem(response.uri, currentDate, response.mediaFileType, DirType.GALLERY)
                    mediaFileViewModel.baseListItem.add(0, item)
                    // Обновляем данные вкладки Галерея, если она текущая и фильтрация соответствующая
                    if (mediaFileViewModel.filterLiveData.value!!.dir == DirType.GALLERY
                        && mediaFileViewModel.filterLiveData.value!!.type == response.mediaFileType) {
                        mediaFileViewModel.filterListItems()
                    }
                }
                is Response.FileExists -> {
                    Toast.makeText(requireContext(), R.string.file_exists_in_galery, Toast.LENGTH_SHORT).show()
                }
                is Response.Error -> {
                    parentFragmentManager.beginTransaction().remove(this@SelectMediaFileFragment).commitAllowingStateLoss() // Закрытие фрагмента
                    val res = when(response.errCode) {
                        ErrCode.OUT_OF_MEMORY -> R.string.out_of_memory
                        ErrCode.COPY_ERROR -> R.string.file_not_saved
                        ErrCode.UNEXPECTED_ERROR -> R.string.file_not_saved
                    }
                    Toast.makeText(requireContext(), res, Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
        }
        // Добавление файла с камеры во общее хранилище
        mediaFileViewModel.getResultAddingFileLiveData().observe(viewLifecycleOwner) { item ->
            if (item != null) {
                requireActivity().runOnUiThread {
                    mediaFileViewModel.filterLiveData.postValue(MediaFileFilter(DirType.GALLERY, null))
                    mediaFileViewModel.baseListItem.add(0, item)
                    mediaFileViewModel.filterListItems()
                    recycler.scrollToPosition(0)
                }
            }
        }

        // Слушаем изменение текущего холдера
        mediaFileViewModel.currentHolderItemLiveData.observe(viewLifecycleOwner) { item ->
            if (item != null) {
                loadMedia(item.uri, item.mediaFileType)
                binding.ivBtnCopyToGallery.isVisible = false
                isVisibleBtnCopyToGallery(item)
            }
        }

        // Слушаем завершился ли процесс копирования файла
        mediaFileViewModel.copyJob.observe(viewLifecycleOwner) { copyJob ->
            val currentItem = mediaFileViewModel.currentHolderItemLiveData.value
            if (copyJob == null) isVisibleBtnCopyToGallery(currentItem)
        }

        // Копирование файлф в галерею
        binding.ivBtnCopyToGallery.setOnClickListener {
            exportFileToMediaStore()
        }

        // Кнопка Камера
        binding.ivPhoto.setOnClickListener {
            requestCameraPermissions()
        }

        // Выбор папки
        binding.tvGallery.setOnClickListener {
            val filter = mediaFileViewModel.filterLiveData.value
            filter?.also {
                it.dir = DirType.GALLERY
                mediaFileViewModel.filterLiveData.postValue(it)
            }
        }
        binding.tvApp.setOnClickListener {
            val filter = mediaFileViewModel.filterLiveData.value
            filter?.also {
                it.dir = DirType.APP
                mediaFileViewModel.filterLiveData.postValue(it)
            }
        }
        // Кнопка размер окна предпросмотра
        binding.ivPreviewSize.setOnClickListener {
            setPreviewWindowSize()
        }
        //Кнопка Выбрать
        binding.tvSelect.setOnClickListener {
            val currentItem = mediaFileViewModel.currentHolderItemLiveData.value
            if (currentItem != null) {
                mediaFileViewModel.saveImageToExternalAppStorage(
                    currentItem.uri,
                    currentItem.mediaFileType,
                    currentItem.dir,
                    false
                )
            } else {
                Toast.makeText(requireContext(), R.string.no_file_selected, Toast.LENGTH_SHORT).show()
            }
        }
    }

    /*private fun requestWriteMediaPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+: нужны явные разрешения на запись медиа
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.WRITE_MEDIA_IMAGES)
            }
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.WRITE_MEDIA_VIDEO)
            }
        }
        // Android 8–12: WRITE_MEDIA_* не существует, но READ_EXTERNAL_STORAGE уже позволяет писать в MediaStore
        // → НЕ запрашиваем ничего дополнительного

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                permissionsToRequest.toTypedArray(),
                REQUEST_WRITE_MEDIA_PERMISSIONS
            )
        } else {
            // Разрешения уже есть — сразу запускаем экспорт
            exportFileToMediaStore()
        }
    }*/

    private fun exportFileToMediaStore() {
        val dialogView =
            LayoutInflater.from(context).inflate(R.layout.dialog_title_attention, null)
        MaterialAlertDialogBuilder(context)
            .setCustomTitle(dialogView)
            .setMessage(getString(R.string.copy_dialog_hint))
            .setNeutralButton(getString(R.string.back)) { _, _ -> }
            .setPositiveButton(getString(R.string.copy)) { _, _ ->
                val item = mediaFileViewModel.currentHolderItemLiveData.value!!
                mediaFileViewModel.copyFileFromExternalAppToPublicStorage(item.uri, item.mediaFileType)
            }
            .show()
    }

    private fun requestMediaPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {  // API 33+
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
            /*if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_VIDEO)
            }*/
        } else {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(requireActivity(), permissionsToRequest.toTypedArray(), REQUEST_MEDIA_PERMISSIONS)
            parentFragmentManager.beginTransaction().remove(this@SelectMediaFileFragment).commitAllowingStateLoss()
        } else {
            updateRecyclerView()
        }
    }

    private fun requestCameraPermissions() {
        val permissions = mutableListOf<String>()
        permissions.add(Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {   // Android 9 и ниже - нужно WRITE_EXTERNAL_STORAGE
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        // Проверяем, все ли разрешения уже получены
        val permissionsToRequest = permissions.filter { permission ->
            ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()
        if (permissionsToRequest.isEmpty()) {
            openCameraForPhoto()
        } else {
            ActivityCompat.requestPermissions(requireActivity(), permissionsToRequest, REQUEST_CAMERA_PERMISSIONS)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_MEDIA_PERMISSIONS -> {
                val allGranted = grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
                if (allGranted) {
                    updateRecyclerView()
                } else {
                    view?.let {
                        Snackbar.make(
                            it.findViewById(android.R.id.content),
                            context.resources.getString(R.string.access_media_denied),
                            Snackbar.LENGTH_LONG
                        )
                    }?.setAction(context.resources.getString(R.string.settings)) {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        startActivity(intent)
                    }?.show()
                    parentFragmentManager.beginTransaction().remove(this@SelectMediaFileFragment)
                        .commitAllowingStateLoss()
                }
            }
            REQUEST_CAMERA_PERMISSIONS -> {
                val granted = grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
                if (granted) {
                    openCameraForPhoto()
                } else {
                    view?.let {
                        Snackbar.make(
                            it.findViewById(android.R.id.content),
                            context.resources.getString(R.string.access_camera_denied),
                            Snackbar.LENGTH_LONG
                        )
                    }?.setAction(context.resources.getString(R.string.settings)) {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        startActivity(intent)
                    }?.show()
                }
            }
        }
    }

    private fun updateRecyclerView() {
        mediaFileViewModel.getAllMediaFiles()
    }

    private fun openCameraForPhoto() {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures")
            } else {
                // Для API 26-28: создаем путь к файлу
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val file = File(picturesDir, "${System.currentTimeMillis()}.jpg")
                put(MediaStore.Images.Media.DATA, file.absolutePath)
            }
        }
        photoUri = requireContext().contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values
        )

        photoUri?.let { cameraUri ->
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, cameraUri)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
            photoLauncher.launch(intent)
        }
    }

    private fun updatingMainActivity(fileName: String) {
        // Обновляем данные в MainActivity
        val records = ma.adapter.records
        val position = records.indexOfFirst { it.id == idString }
        // Обновление данных в массиве
        val item = records[position]
        item.mediaFile = fileName
        // Обновление холдера
        ma.adapter.notifyItemChanged(position)
    }

    private fun selectedDir(tv: TextView) {
        unselectedDir(binding.tvGallery)
        unselectedDir(binding.tvApp)
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
        tv.typeface = Typeface.defaultFromStyle(Typeface.BOLD)
    }
    private fun unselectedDir(tv: TextView) {
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        tv.typeface = Typeface.defaultFromStyle(Typeface.NORMAL)
    }

    // Проверка, выводить ли на экран кнопку Копирование в галерею
    private fun isVisibleBtnCopyToGallery(currentItem: MediaItem?) {
        var result = false
        if (currentItem != null) {
            if (currentItem.dir == DirType.APP && mediaFileViewModel.copyJob.value == null) {
                result = !isFileExistsInPublicStorage(currentItem.uri)  // Проверяем есть ли такой файл в Галерее
            }
        }
        binding.ivBtnCopyToGallery.isVisible = result
    }
    private fun isFileExistsInPublicStorage(uri: Uri): Boolean {
        val fileName = getFileName(uri)
        if (fileName.isBlank()) return false
        return mediaFileViewModel.isTheFileInPublicStorage(fileName)
    }
    private fun getFileName(uri: Uri): String {
        val fileName = when (uri.scheme) {
            "file" -> {
                File(uri.path ?: "").name
            }
            else -> {
                uri.lastPathSegment ?: ""   // берем последний сегмент
            }
        }
        return if (fileName.contains("#")) {
            fileName.substringAfter("#")    // отделяем наш префикс
        } else {
            fileName
        }
    }

    override fun getVM(): MediaFileViewModel = mediaFileViewModel
    override fun onScrollStateChanged(scrollState: ScrollState) {
        when (scrollState) {
            ScrollState.DOWN -> {           // Прокрутка вниз
                this.scrollState = scrollState
                binding.ivBtnScroll.setImageResource(R.drawable.ic_scroll_down)
                binding.ivBtnScroll.isVisible = true
                if (isPreviewContainerBig) setPreviewWindowSize()
            }
            ScrollState.UP -> {             // Прокрутка вверх
                this.scrollState = scrollState
                binding.ivBtnScroll.setImageResource(R.drawable.ic_scroll_up)
                binding.ivBtnScroll.isVisible = true
                if (isPreviewContainerBig) setPreviewWindowSize()
            }
            ScrollState.STOPPED -> {        // Прокрутка остановлена
                this.scrollState = scrollState
            }
            ScrollState.END -> {            // Конец списка
                this.scrollState = scrollState
                binding.ivBtnScroll.isVisible = false
            }
        }
        scrollJob.cancel()
        scrollJob = lifecycleScope.launch {
            delay(1000)
            binding.ivBtnScroll.isVisible = false
        }
    }

    private fun loadMedia(uri: Uri, mediaFileType: MediaFileType) {
        loadMediaJob.cancel()
        loadMediaJob = lifecycleScope.launch {
            when (mediaFileType) {
                MediaFileType.IMAGE -> {
                    binding.photoWindow.isVisible = true
                    try {
                        val bitmap = getBitmapFromUri(context, uri)
                        if (bitmap == null || bitmap.isRecycled) {
                            Toast.makeText(
                                requireContext(),
                                "Ошибка загрузки изображения",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@launch
                        }

                        val rotatedBitmap = rotateBitmapAccordingToExif(context, uri, bitmap)
                        binding.photoWindow.setImage(ImageSource.bitmap(rotatedBitmap))
                        binding.photoWindow.setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CENTER_INSIDE)
                        binding.photoWindow.setOrientation(SubsamplingScaleImageView.ORIENTATION_USE_EXIF)
                        binding.photoWindow.setDoubleTapZoomScale(2f)
                        binding.photoWindow.setPanLimit(SubsamplingScaleImageView.PAN_LIMIT_INSIDE)
                    } catch (_: Exception) {
                        Toast.makeText(
                            requireContext(),
                            "Ошибка загрузки изображения",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                else -> {
                    Toast.makeText(
                        requireContext(),
                        "Неподдерживаемый формат",
                        Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }
    fun getBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream)
        }
    }

    private fun rotateBitmapAccordingToExif(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val exif = ExifInterface(inputStream)
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
            } ?: bitmap // если inputStream == null — возвращаем оригинал
        } catch (_: Exception) {
            bitmap
        }
    }

    // Установка размера окна предпросмотра
    private fun setPreviewWindowSize() {
        var size = 400f
        if (isPreviewContainerBig) {
            size = 200f
            binding.ivPreviewSize.setImageResource(R.drawable.ic_preview_max)
        } else {
            binding.ivPreviewSize.setImageResource(R.drawable.ic_preview_min)
        }
        var widht: Int
        var height: Int
        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            widht = LinearLayout.LayoutParams.MATCH_PARENT
            height = dpToPx(context, size)
        } else {
            widht = dpToPx(context, size)
            height = LinearLayout.LayoutParams.MATCH_PARENT
        }
        binding.flPreviewContainer.layoutParams = LinearLayout.LayoutParams(widht, height)
        isPreviewContainerBig = !isPreviewContainerBig
    }
    private fun dpToPx(context: Context, dp: Float): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density).toInt()
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
        _binding = null
    }

    private fun checkUriReady(uri: Uri): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val buffer = ByteArray(1024)
                val bytesRead = inputStream.read(buffer)
                bytesRead > 0
            } ?: false
        } catch (_: Exception) {
            false
        }
    }

    companion object {
        const val REQUEST_MEDIA_PERMISSIONS = 1001
        const val REQUEST_CAMERA_PERMISSIONS = 1002
        const val ARG_ID = "id"
        @JvmStatic
        fun newInstance(id: Long) =
            SelectMediaFileFragment().apply { arguments = Bundle().apply { putLong(ARG_ID, id) } }
    }
}