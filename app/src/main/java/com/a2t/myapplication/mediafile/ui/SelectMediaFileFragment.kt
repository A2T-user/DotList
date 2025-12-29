package com.a2t.myapplication.mediafile.ui

import android.Manifest
import android.app.Activity
import android.content.ContentValues
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
import com.a2t.myapplication.mediafile.data.model.MediaType
import com.a2t.myapplication.mediafile.presentation.MediaFileViewModel
import com.a2t.myapplication.mediafile.ui.recycler.MediaFileAdapter
import com.a2t.myapplication.mediafile.ui.recycler.MediaFileAdapterCallback
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.File
import java.io.FileInputStream
import kotlin.getValue


class SelectMediaFileFragment : Fragment(), MediaFileAdapterCallback {
    private val mediaFileViewModel: MediaFileViewModel by viewModel()
    private var _binding: FragmentSelectMediaFileBinding? = null
    private val binding get() = _binding!!
    private lateinit var context: Context
    private lateinit var ma: MainActivity
    private var idString: Long? = null
    private var selectedType = MediaType.ALL
    private lateinit var recycler: RecyclerView
    lateinit var adapter: MediaFileAdapter
    private var isShownSelectionMenu = false
    private var isStart = true
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
        selectedType = savedInstanceState?.getString(SELECTED_TYPE)?.let { MediaType.valueOf(it) } ?: MediaType.ALL
        val res = when(selectedType) {
            MediaType.PHOTO -> R.string.photo
            MediaType.VIDEO -> R.string.video
            MediaType.ALL -> R.string.all
        }
        binding.tvSelectedType.setText(res)
        val savedPosition = savedInstanceState?.getInt(CURRENT_HOLDER_POSITION, -1) ?: -1
        if (savedPosition != -1) {
            adapter.currentHolderPositionLiveData.value = savedPosition
        }

        requestMediaPermissions()

        binding.llSelectType.setOnClickListener {
            showSelectionMenu(!isShownSelectionMenu)
            selectionMenuJob = viewLifecycleOwner.lifecycleScope.launch {
                delay(3000)
                showSelectionMenu(false)
            }
        }

        binding.tvAll.setOnClickListener {
            selectedType = MediaType.ALL
            binding.tvSelectedType.setText(R.string.all)
            showSelectionMenu(false)
            requestMediaPermissions()
        }

        binding.tvPhoto.setOnClickListener {
            selectedType = MediaType.PHOTO
            binding.tvSelectedType.setText(R.string.photo)
            showSelectionMenu(false)
            requestMediaPermissions()
        }

        binding.tvVideo.setOnClickListener {
            selectedType = MediaType.VIDEO
            binding.tvSelectedType.setText(R.string.video)
            showSelectionMenu(false)
            requestMediaPermissions()
        }


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
            if (isStart) {
                startRecyclerView()
                isStart = false
            } else {
                updateRecyclerView()
            }
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
                    if (isStart) {
                        startRecyclerView()
                    } else {
                        updateRecyclerView()
                    }
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

    private fun startRecyclerView() {
        binding.progressBar.isVisible = true
        mediaFileViewModel.getAllMediaFiles(selectedType) { itemList ->
            adapter.itemList.clear()
            adapter.itemList.addAll(itemList)
            recycler.adapter = adapter
            recycler.layoutManager = GridLayoutManager(requireContext(), 3)
            recycler.itemAnimator = DefaultItemAnimator()
            recycler.scheduleLayoutAnimation()
            recycler.invalidate()
        }
        binding.progressBar.isVisible = false
    }

    private fun updateRecyclerView() {
        adapter.currentHolderPositionLiveData.postValue(-1) // Сбрасываем текущий холдер
        binding.progressBar.isVisible = true
        mediaFileViewModel.getAllMediaFiles(selectedType) { itemList ->
            adapter.itemList.clear()
            adapter.itemList.addAll(itemList)
            adapter.notifyDataSetChanged()
        }
        binding.progressBar.isVisible = false
    }

    private fun showSelectionMenu(isShow: Boolean) {
        selectionMenuJob?.cancel()
        selectionMenuJob = null
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

    private suspend fun addPhotoToGallery(file: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "photo_${System.currentTimeMillis()}.jpg")
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/YourApp")  // Папка в Pictures
            }
            val contentResolver = requireContext().contentResolver
            val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            val outputStream = contentResolver.openOutputStream(uri!!)
            FileInputStream(file).use { input -> input.copyTo(outputStream!!) }
            outputStream?.close()
            // Уведомляем систему об изменении (осталяет для совместимости)
            contentResolver.notifyChange(uri, null)
            MediaScannerConnection.scanFile(requireContext(), arrayOf(uri.toString()), null, null)
            true
        } catch (e: Exception) {
            e.printStackTrace()  // Логируем ошибку
            false
        }
    }

    fun processAndSaveImage(originalFile: File) {
        lifecycleScope.launch {
            if (addPhotoToGallery(originalFile)) {
                // Если успешно, удаляем временный файл
                originalFile.delete()
                withContext(Dispatchers.Main) {
                    delay(1000L)
                    updateRecyclerView()  // Обновляем UI
                }
            }
        }
    }

    private suspend fun addVideoToGallery(file: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "video_${System.currentTimeMillis()}.mp4")
                put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/YourApp")  // Папка в Movies
            }
            val contentResolver = requireContext().contentResolver
            val uri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
            val outputStream = contentResolver.openOutputStream(uri!!)
            FileInputStream(file).use { input -> input.copyTo(outputStream!!) }
            outputStream?.close()
            // Уведомляем систему об изменении (для совместимости)
            contentResolver.notifyChange(uri, null)
            MediaScannerConnection.scanFile(requireContext(), arrayOf(uri.toString()), null, null)
            true
        } catch (e: Exception) {
            e.printStackTrace()  // Логируем ошибку
            false
        }
    }

    fun processAndSaveVideo(originalFile: File) {
        lifecycleScope.launch {
            if (addVideoToGallery(originalFile)) {
                // Если успешно, удаляем временный файл
                originalFile.delete()
                withContext(Dispatchers.Main) {
                    delay(1000L)  // Задержка для индексации (тестируйте)
                    updateRecyclerView()  // Обновляем UI
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        ma.mainBackPressedCallback.isEnabled = false
    }

    override fun onStop() {
        super.onStop()
        ma.mainBackPressedCallback.isEnabled = true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putString(SELECTED_TYPE, selectedType.name)
        val position = adapter.currentHolderPositionLiveData.value ?: -1
        outState.putInt(CURRENT_HOLDER_POSITION, position)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        selectionMenuJob?.cancel()
        _binding = null
    }

    companion object {
        const val REQUEST_MEDIA_PERMISSIONS = 1001
        const val REQUEST_CAMERA_PERMISSIONS = 1002
        const val SELECTED_TYPE = "selectedType"
        const val CURRENT_HOLDER_POSITION = "currentHolderPosition"
        const val ARG_ID = "id"
        @JvmStatic
        fun newInstance(id: Long) =
            SelectMediaFileFragment().apply { arguments = Bundle().apply { putLong(ARG_ID, id) } }
    }
}