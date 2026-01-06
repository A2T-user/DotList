package com.a2t.myapplication.mediafile.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.a2t.myapplication.mediafile.data.dto.ErrCode
import com.a2t.myapplication.mediafile.data.dto.Response
import com.a2t.myapplication.mediafile.data.model.MediaType
import com.a2t.myapplication.mediafile.presentation.MediaFileViewModel
import com.a2t.myapplication.mediafile.ui.recycler.MediaFileAdapter
import com.a2t.myapplication.mediafile.ui.recycler.MediaFileAdapterCallback
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.File
import kotlin.getValue


class SelectMediaFileFragment : Fragment(), MediaFileAdapterCallback {
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
        // Следим за изменение selectedType
        mediaFileViewModel.getSelectedTypeLiveData().observe(viewLifecycleOwner) { selectedType ->
            val res = when(selectedType) {
                MediaType.PHOTO -> R.string.photo
                MediaType.VIDEO -> R.string.video
                MediaType.ALL -> R.string.all
            }
            binding.tvSelectedType.setText(res)
            requestMediaPermissions()
        }
        // Сохранение файла во внутреннем хранилище
        mediaFileViewModel.getResponseLiveData().observe(viewLifecycleOwner) { response ->
            binding.progressBar.isVisible = true
            parentFragmentManager.beginTransaction().remove(this@SelectMediaFileFragment).commitAllowingStateLoss() // Закрытие фрагмента
            when(response) {
                is Response.Success -> {
                    val fileName = response.fileName
                    // Обновляем строку БД
                    mediaFileViewModel.addMediaFile(idString!!, fileName)
                    // Обновляем данные в MainActivity
                    updatingMainActivity(fileName)
                }
                is Response.FileExists -> {
                    val fileName = response.fileName
                    // Обновляем строку БД
                    mediaFileViewModel.addMediaFile(idString!!, fileName)
                    // Обновляем данные в MainActivity
                    updatingMainActivity(fileName)
                }
                is Response.Error -> {
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
        mediaFileViewModel.getResultAddingFileLiveData().observe(viewLifecycleOwner) { uri ->
            if (uri != null) {
                val resolver = requireContext().contentResolver
                val projection = arrayOf(MediaStore.MediaColumns.DATA)
                val cursor = resolver.query(uri, projection, null, null, null)
                cursor?.use { c ->
                    if (c.moveToFirst()) {
                        val filePath = c.getString(c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA))
                        MediaScannerConnection.scanFile(
                            requireContext(),
                            arrayOf(filePath),
                            null
                        ) { _, _ ->
                            requireActivity().runOnUiThread {
                                updateRecyclerView()
                            }
                        }
                    }
                }
                cursor?.close()
            }
            binding.progressBar.isVisible = false
        }

        // Следим за обновлением массива данных рециклера
        mediaFileViewModel.getItemListLiveData().observe(viewLifecycleOwner) { newList ->
            adapter.itemList.clear()
            adapter.itemList.addAll(newList)
            adapter.notifyDataSetChanged()
            binding.progressBar.isVisible = false
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
            mediaFileViewModel.selectedTypeLiveData.postValue( MediaType.ALL)
            showSelectionMenu(false)
        }
        binding.tvPhoto.setOnClickListener {
            mediaFileViewModel.selectedTypeLiveData.postValue( MediaType.PHOTO)
            showSelectionMenu(false)
        }
        binding.tvVideo.setOnClickListener {
            mediaFileViewModel.selectedTypeLiveData.postValue( MediaType.VIDEO)
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
        binding.tvSelect.setOnClickListener {
            binding.progressBar.isVisible = true
            val currentItem = mediaFileViewModel.currentHolderItemLiveData.value
            if (currentItem != null) {

                mediaFileViewModel.saveImageToPrivateStorage(
                    currentItem.uri,
                    currentItem.mediaFileType,
                    binding.cbTransfer.isChecked
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
        binding.progressBar.isVisible = true
        mediaFileViewModel.getAllMediaFiles()
    }

    private fun showSelectionMenu(isShow: Boolean) {
        selectionMenuJob?.cancel()
        selectionMenuJob = null
        val selectedType: MediaType = mediaFileViewModel.selectedTypeLiveData.value!!
        if (isShow) {
            binding.tvAll.isVisible = selectedType != MediaType.ALL
            binding.tvPhoto.isVisible = selectedType != MediaType.PHOTO
            binding.tvVideo.isVisible = selectedType != MediaType.VIDEO
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
        videoFile = File(requireContext().getExternalFilesDir(Environment.DIRECTORY_DCIM), "temp_video_${System.currentTimeMillis()}.mp4")  // DCIM для видео
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
        binding.progressBar.isVisible = true
        mediaFileViewModel.addPhotoToGallery(file)
    }

    private fun addVideoToGallery(file: File) {
        binding.progressBar.isVisible = true
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
    companion object {
        const val REQUEST_MEDIA_PERMISSIONS = 1001
        const val REQUEST_CAMERA_PERMISSIONS = 1002
        const val ARG_ID = "id"
        @JvmStatic
        fun newInstance(id: Long) =
            SelectMediaFileFragment().apply { arguments = Bundle().apply { putLong(ARG_ID, id) } }
    }
}