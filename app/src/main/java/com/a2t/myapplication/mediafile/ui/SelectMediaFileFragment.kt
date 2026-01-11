package com.a2t.myapplication.mediafile.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.media.MediaScannerConnection
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
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
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
import com.a2t.myapplication.mediafile.presentation.MediaFileViewModel
import com.a2t.myapplication.mediafile.presentation.model.MediaFileFilter
import com.a2t.myapplication.mediafile.ui.recycler.MediaFileAdapter
import com.a2t.myapplication.mediafile.ui.recycler.MediaFileAdapterCallback
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.File
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
    private var isShownSelectionMenu = false
    private var isVideo = true
    private var isMenuCameraFull = false
    private lateinit var photoLauncher: ActivityResultLauncher<Intent>
    private lateinit var videoLauncher: ActivityResultLauncher<Intent>
    private var photoFile: File? = null
    private var videoFile: File? = null
    private var selectionMenuJob: Job? = null
    private var scrollJob = lifecycleScope.launch {}
    private var scrollState = ScrollState.STOPPED

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        idString = arguments?.getLong(ARG_ID)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        photoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && photoFile != null && photoFile!!.exists()) {
                // Запускаем асинхронную обработку вместо блока UI
                processAndSaveImage(photoFile!!)
            }
            photoFile = null
        }
        videoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && videoFile != null && videoFile!!.exists()) {
                // Запускаем асинхронную обработку
                processAndSaveVideo(videoFile!!)
            }
            videoFile = null
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
            val res = when(filter.type) {
                MediaFileType.IMAGE -> R.string.photo
                MediaFileType.VIDEO -> R.string.video
                else ->  R.string.all
            }
            binding.tvSelectedType.setText(res)
            mediaFileViewModel.filterListItems()
        }

        // Следим за сохранением файла во внутреннем хранилище
        mediaFileViewModel.getResponseLiveData().observe(viewLifecycleOwner) { response ->
            when(response) {
                is Response.Success -> {
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
                            val originalName = response.originalName
                            mediaFileViewModel.filterExistingFiles(originalName)
                        }
                        .setPositiveButton(getString(R.string.new_copy)) { _, _ ->
                            mediaFileViewModel.saveImageToPrivateStorage(
                                response.uri,
                                response.mediaFileType,
                                binding.cbTransfer.isChecked,
                                true
                                )
                        }
                        .show()
                }
                is Response.Error -> {
                    parentFragmentManager.beginTransaction().remove(this@SelectMediaFileFragment).commitAllowingStateLoss() // Закрытие фрагмента
                    val res = when(response.errCode) {
                        ErrCode.OUT_OF_MEMORY -> R.string.out_of_memory
                        ErrCode.COPY_ERROR -> R.string.copy_error
                        ErrCode.UNEXPECTED_ERROR -> R.string.unexpected_error
                    }
                    Toast.makeText(requireContext(), res, Toast.LENGTH_SHORT).show()
                }
            }
        }
        // Добавление файла с камеры в галерею
        mediaFileViewModel.getResultAddingFileLiveData().observe(viewLifecycleOwner) { item ->
            if (item != null) {
                val resolver = requireContext().contentResolver
                val projection = arrayOf(MediaStore.MediaColumns.DATA)
                val cursor = resolver.query(item.uri, projection, null, null, null)
                cursor?.use { c ->
                    if (c.moveToFirst()) {
                        val filePath = c.getString(c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA))
                        MediaScannerConnection.scanFile(
                            requireContext(),
                            arrayOf(filePath),
                            null
                        ) { _, _ ->
                            requireActivity().runOnUiThread {
                                mediaFileViewModel.filterLiveData.postValue(MediaFileFilter(DirType.GALLERY, null))
                                mediaFileViewModel.baseListItem.add(0, item)
                                mediaFileViewModel.filterListItems()
                                mediaFileViewModel.currentHolderItemLiveData.postValue(item)
                                recycler.scrollToPosition(0)
                            }
                        }
                    }
                }
                cursor?.close()
            }
        }

        // Меню выбора типа контента
        binding.llSelectType.setOnClickListener {
            showSelectionMenu(!isShownSelectionMenu)
            selectionMenuJob = viewLifecycleOwner.lifecycleScope.launch {
                delay(3000)
                showSelectionMenu(false)
            }
        }
        binding.tvAll.setOnClickListener {
            val filter = mediaFileViewModel.filterLiveData.value
            filter?.also {
                it.type = null
                mediaFileViewModel.filterLiveData.postValue(it)
            }
            showSelectionMenu(false)
        }
        binding.tvPhoto.setOnClickListener {
            val filter = mediaFileViewModel.filterLiveData.value
            filter?.also {
                it.type = MediaFileType.IMAGE
                mediaFileViewModel.filterLiveData.postValue(it)
            }
            showSelectionMenu(false)
        }
        binding.tvVideo.setOnClickListener {
            val filter = mediaFileViewModel.filterLiveData.value
            filter?.also {
                it.type = MediaFileType.VIDEO
                mediaFileViewModel.filterLiveData.postValue(it)
            }
            showSelectionMenu(false)
        }

        // Меню камеры, выбор фото/видео
        binding.ivPhoto.setOnClickListener {
            if (isMenuCameraFull) {
                isVideo = false
                binding.ivVideo.isVisible = false
                isMenuCameraFull = false
                requestCameraPermissions()
            } else {
                binding.ivVideo.isVisible = true
                isMenuCameraFull = true
                viewLifecycleOwner.lifecycleScope.launch {
                    delay(3000)
                    binding.ivVideo.isVisible = false
                    isMenuCameraFull = false
                }
            }
        }
        binding.ivVideo.setOnClickListener {
            isVideo = true
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

        binding.tvSelect.setOnClickListener {
            val currentItem = mediaFileViewModel.currentHolderItemLiveData.value
            if (currentItem != null) {
                mediaFileViewModel.saveImageToPrivateStorage(
                    currentItem.uri,
                    currentItem.mediaFileType,
                    binding.cbTransfer.isChecked,
                    false
                )
            } else {
                Toast.makeText(requireContext(), R.string.no_file_selected, Toast.LENGTH_SHORT).show()
            }
        }
        // Копирование/перенос
        binding.cbCopy.setOnClickListener {
            binding.cbCopy.isChecked = true
            binding.cbTransfer.isChecked = false
        }
        binding.cbTransfer.setOnClickListener {
            binding.cbCopy.isChecked = false
            binding.cbTransfer.isChecked = true
        }
    }

    private fun requestMediaPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {  // API 33+
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_VIDEO)
            }
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
        val permissions = arrayOf(Manifest.permission.CAMERA)
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            if (isVideo) {
                openCameraForVideo()
            } else {
                openCameraForPhoto()
            }
        } else {
            ActivityCompat.requestPermissions(requireActivity(), permissions, REQUEST_CAMERA_PERMISSIONS)
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
                            it.findViewById(android.R.id.content),  // Или ваш View
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
                val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
                if (granted) {
                    if (isVideo) {
                        openCameraForVideo()
                    } else {
                        openCameraForPhoto()
                    }
                } else {
                    view?.let {
                        Snackbar.make(
                            it.findViewById(android.R.id.content),  // Или ваш View
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

    private fun showSelectionMenu(isShow: Boolean) {
        selectionMenuJob?.cancel()
        selectionMenuJob = null
        val selectedType = mediaFileViewModel.filterLiveData.value!!.type
        if (isShow) {
            binding.tvAll.isVisible = selectedType != null
            binding.tvPhoto.isVisible = selectedType != MediaFileType.IMAGE
            binding.tvVideo.isVisible = selectedType != MediaFileType.VIDEO
            isShownSelectionMenu = true
        } else {
            binding.tvAll.isVisible = false
            binding.tvPhoto.isVisible = false
            binding.tvVideo.isVisible = false
            isShownSelectionMenu = false
        }
    }

    private fun openCameraForPhoto() {
        photoFile = File(requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES), "temp_photo_${System.currentTimeMillis()}.jpg")
        val photoUri = FileProvider.getUriForFile(
            requireContext(),
            "com.a2t.myapplication.fileprovider",  // Authority из манифеста
            photoFile!!
        )
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
        }
        photoLauncher.launch(intent)
    }

    private fun openCameraForVideo() {
        videoFile = File(requireContext().getExternalFilesDir(Environment.DIRECTORY_DCIM), "temp_video_${System.currentTimeMillis()}.mp4")
        val videoUri = FileProvider.getUriForFile(
            requireContext(),
            "com.a2t.myapplication.fileprovider",
            videoFile!!
        )
        val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, videoUri)
            // Ограничения на качество/длительность
            putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1)  // 0 - low, 1 - high
            putExtra(MediaStore.EXTRA_DURATION_LIMIT, 30)  // 30 сек макс
        }
        videoLauncher.launch(intent)
    }

    private fun addPhotoToGallery(file: File) {
        mediaFileViewModel.addPhotoToGallery(file)
    }

    private fun addVideoToGallery(file: File) {
        mediaFileViewModel.addVideoToGallery(file)
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


    fun processAndSaveImage(originalFile: File) {
        addPhotoToGallery(originalFile)
    }

    fun processAndSaveVideo(originalFile: File) {
        addVideoToGallery(originalFile)
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
        selectionMenuJob?.cancel()
        _binding = null
    }

    override fun getVM(): MediaFileViewModel = mediaFileViewModel
    override fun onScrollStateChanged(scrollState: ScrollState) {
        when (scrollState) {
            ScrollState.DOWN -> {           // Прокрутка вниз
                this.scrollState = scrollState
                binding.ivBtnScroll.setImageResource(R.drawable.ic_scroll_down)
                binding.ivBtnScroll.isVisible = true
            }
            ScrollState.UP -> {             // Прокрутка вверх
                this.scrollState = scrollState
                binding.ivBtnScroll.setImageResource(R.drawable.ic_scroll_up)
                binding.ivBtnScroll.isVisible = true
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

    companion object {
        const val REQUEST_MEDIA_PERMISSIONS = 1001
        const val REQUEST_CAMERA_PERMISSIONS = 1002
        const val ARG_ID = "id"
        @JvmStatic
        fun newInstance(id: Long) =
            SelectMediaFileFragment().apply { arguments = Bundle().apply { putLong(ARG_ID, id) } }
    }
}