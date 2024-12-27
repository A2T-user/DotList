package com.a2t.myapplication.main.ui.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.icu.util.Calendar
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.a2t.myapplication.App
import com.a2t.myapplication.R
import com.a2t.myapplication.main.ui.NotificationWorker
import com.a2t.myapplication.databinding.ButtonAlarmBinding
import com.a2t.myapplication.databinding.FragmentAlarmBinding
import com.a2t.myapplication.main.ui.activity.MainActivity
import com.a2t.myapplication.main.domain.model.ListRecord
import com.a2t.myapplication.main.presentation.MainViewModel
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import java.util.UUID
import java.util.concurrent.TimeUnit

const val ALARM_TIME = "ALARM_TIME"
const val ALARM_DATE = "ALARM_DATE"
const val ALARM_DATE_TIME = "ALARM_DATE_TIME"
const val ALARM_TEXT = "ALARM_TEXT"

class AlarmFragment : Fragment() {

    private val mainViewModel: MainViewModel by activityViewModel()
    private var _binding: FragmentAlarmBinding? = null
    private val binding get() = _binding!!
    private lateinit var buttonBinding: ButtonAlarmBinding
    private var record: ListRecord? = null
    private var alarmDateTime: Long? = null
    private var alarmText: String? = null
    private var alarmDate: Long? = null
    private var alarmTime: Long? = null
    private val calendar = Calendar.getInstance()
    private val offset = calendar.timeZone.rawOffset
    private var nameJob = lifecycleScope.launch {}
    private lateinit var permissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Инициализация launcher для запроса разрешения
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (!isGranted) {
                // Разрешение отклонено, уведомления не будут отправлены
                Toast.makeText(requireContext(), getString(R.string.ban_notifications), Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAlarmBinding.inflate(layoutInflater)
        buttonBinding = binding.buttonAlarm
        record = mainViewModel.record
        alarmDateTime = record?.alarmTime ?: System.currentTimeMillis()
        alarmText = record?.alarmText ?: record?.record
        alarmTime = alarmDateTime!! % 86400000
        alarmDate = alarmDateTime!! - alarmTime!!

        savedInstanceState?.let {
            val savedAlarmTime = savedInstanceState.getLong(ALARM_TIME, -1)
            val savedAlarmDate = savedInstanceState.getLong(ALARM_DATE, -1)
            val savedAlarmDateTime = savedInstanceState.getLong(ALARM_DATE_TIME, -1)
            alarmTime = if (savedAlarmTime != -1L) savedAlarmTime else null
            alarmDate = if (savedAlarmDate != -1L) savedAlarmDate else null
            alarmDateTime = if (savedAlarmDateTime != -1L) savedAlarmDateTime else null
            alarmText = savedInstanceState.getString(ALARM_TEXT)
        }

        return binding.root
    }

    @SuppressLint("ScheduleExactAlarm")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        App.getTextSizeLiveData().observe(viewLifecycleOwner) { size ->
            binding.etText.textSize = size
            binding.tvDate.textSize = size
            binding.tvTime.textSize = size
        }

        binding.etText.setText(alarmText)
        alarmDate?.let { binding.tvDate.text = DateFormat.format("dd.M.yy", it).toString() }
        alarmTime?.let { binding.tvTime.text = DateFormat.format("HH:mm", it).toString() }

        activateButtons()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        binding.etText.addTextChangedListener(
            afterTextChanged = { s: Editable? ->
                activateButtons()
                nameJob.cancel()
                if (s?.isEmpty() == true) {
                    binding.etText.setBackgroundResource(R.drawable.field_red)
                    nameJob = lifecycleScope.launch {
                        delay(3000)
                        binding.etText.setText(record?.record)
                    }
                } else {
                    alarmText = s.toString()
                    binding.etText.setBackgroundResource(R.drawable.field_blue)
                }
            }

        )

        binding.tvDate.addTextChangedListener(
            afterTextChanged = {
                dateVerification()
                activateButtons()
            }
        )

        binding.tvTime.addTextChangedListener(
            afterTextChanged = {
                dateVerification()
                activateButtons()
            }
        )
        binding.tvDate.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.select_date)
                .build()
            datePicker.addOnPositiveButtonClickListener {
                alarmDate = datePicker.selection
                alarmDateTime = (alarmTime ?: 0L).let { alarmDate?.plus(it) }
                binding.tvDate.text = DateFormat.format("dd.M.yy", alarmDate!!).toString()
            }
            datePicker.show(requireActivity().supportFragmentManager, "date_picker")
        }

        binding.tvTime.setOnClickListener {
            val currentTime = System.currentTimeMillis() + offset
            val currentHour = (currentTime / 3600000) % 24
            val currentMinutes = (currentTime / 60000) % 60
            val timePicker = MaterialTimePicker.Builder()
                .setTitleText(R.string.select_time)
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setInputMode(MaterialTimePicker.INPUT_MODE_KEYBOARD)
                .setHour(currentHour.toInt())
                .setMinute(currentMinutes.toInt())
                .build()

            timePicker.addOnPositiveButtonClickListener {
                val hour = timePicker.hour
                val minute = timePicker.minute
                alarmTime = (hour * 60 + minute) * 60000L - offset
                alarmDateTime = (alarmDate ?: 0L).let { alarmTime?.plus(it) }
                binding.tvTime.text = DateFormat.format("HH:mm", alarmDateTime!!).toString()
            }
            timePicker.show(requireActivity().supportFragmentManager, "time_picker")
        }

        buttonBinding.btnSave.setOnClickListener {
            var workRequestId: UUID? = null
            // Проверка напоминания
            if (alarmText?.isEmpty() != null) {
                if (dateVerification()) {
                    removeAlarm()           // Удалить предидущее напоминание
                    // Создание нового напоминания
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                            // Разрешение уже предоставлено, можете отправлять уведомления
                            workRequestId = sendNotification()
                        }   // Если разрешение не предоставлено, уведомление не отправляется
                    } else {
                        // Для версий ниже Android 13 разрешение не требуется
                        workRequestId = sendNotification()
                    }
                    // Сохранение нового напоминания в БД
                    record?.alarmTime = alarmDateTime
                    record?.alarmText = alarmText
                    record?.alarmId = workRequestId
                    record?.let { it1 -> mainViewModel.updateRecord(it1) {} }
                    (requireActivity() as MainActivity).cancelCurrentHolder()
                    requireActivity().supportFragmentManager.popBackStack()
                }
            } else {
                Toast.makeText(requireContext(), getString(R.string.text_err), Toast.LENGTH_SHORT).show()
            }
        }

        buttonBinding.btnDel.setOnClickListener {
            removeAlarm()
            record?.alarmTime = null
            record?.alarmText = null
            record?.alarmId = null
            record?.let { mainViewModel.updateRecord(it) {} }
            (requireActivity() as MainActivity).cancelCurrentHolder()
            requireActivity().supportFragmentManager.popBackStack()
        }

        buttonBinding.btnCancel.setOnClickListener {
            (requireActivity() as MainActivity).cancelCurrentHolder()
            requireActivity().supportFragmentManager.popBackStack()
        }
    }

    private fun sendNotification(): UUID? {
        var workRequestId: UUID? = null
        val inputData = record?.let {
            Data.Builder()
                .putLong("IDDIR", it.idDir)
                .putString("ALARM_TEXT", alarmText)
                .build()
        }
        val delayInMillis = alarmDateTime?.minus(System.currentTimeMillis())
        if (inputData != null && delayInMillis != null) {
            val workRequest = OneTimeWorkRequest.Builder(NotificationWorker::class.java)
                .setInputData(inputData)
                .setInitialDelay(delayInMillis, TimeUnit.MILLISECONDS)
                .build()
            workRequestId = workRequest.id
            WorkManager.getInstance(requireContext()).enqueue(workRequest)
        }
        return workRequestId
    }

    private fun removeAlarm() {
        if (record?.alarmId != null) {
            val workManager = WorkManager.getInstance(requireContext())
            workManager.cancelWorkById(record?.alarmId!!)
        }
    }

    private fun dateVerification(): Boolean {
        if (alarmDateTime!! < System.currentTimeMillis() + 60000) {
            binding.tvDate.setBackgroundResource(R.drawable.field_red)
            binding.tvTime.setBackgroundResource(R.drawable.field_red)
            binding.tvAlarmErr.isVisible = true
            binding.tvNow.isVisible = true
            binding.tvNow.text = getString(R.string.now, DateFormat.format("dd.M.yy   HH:mm", System.currentTimeMillis()))
            return false
        } else {
            binding.tvDate.setBackgroundResource(R.drawable.field_blue)
            binding.tvTime.setBackgroundResource(R.drawable.field_blue)
            binding.tvAlarmErr.isVisible = false
            binding.tvNow.isVisible = false
            return true
        }
    }

    private fun activateButtons() {
        with(buttonBinding.btnSave) {
            isEnabled = alarmDateTime != record?.alarmTime || alarmText != record?.alarmText
            alpha = if (isEnabled) 1.0f else 0.3f
        }
        with(buttonBinding.btnDel) {
            isEnabled = record?.alarmTime != null
            alpha = if (isEnabled) 1.0f else 0.3f
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Сохраняем параметры в savedInstanceState. Используем -1 для обозначения null
        outState.putLong(ALARM_TIME, alarmTime ?: -1)
        outState.putLong(ALARM_DATE, alarmDate ?: -1)
        outState.putLong(ALARM_DATE_TIME, alarmDateTime ?: -1)
        outState.putString(ALARM_TEXT, alarmText)
    }

    override fun onStart() {
        super.onStart()
        (requireActivity() as MainActivity).mainBackPressedCallback.isEnabled = false
    }

    override fun onStop() {
        super.onStop()
        (requireActivity() as MainActivity).mainBackPressedCallback.isEnabled = true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        (requireActivity() as MainActivity).cancelCurrentHolder()
    }
}