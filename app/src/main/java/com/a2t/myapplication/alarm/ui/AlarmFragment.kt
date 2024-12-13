package com.a2t.myapplication.alarm.ui

import android.icu.util.Calendar
import android.os.Bundle
import android.text.Editable
import android.text.format.DateFormat
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.a2t.myapplication.App
import com.a2t.myapplication.R
import com.a2t.myapplication.databinding.ButtonAlarmBinding
import com.a2t.myapplication.databinding.FragmentAlarmBinding
import com.a2t.myapplication.root.domain.model.ListRecord
import com.a2t.myapplication.root.presentation.SharedViewModel
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class AlarmFragment : Fragment() {
    private val sharedViewModel: SharedViewModel by activityViewModel()
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


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAlarmBinding.inflate(layoutInflater)
        buttonBinding = binding.buttonAlarm
        record = sharedViewModel.record
        alarmDateTime = record?.alarmTime ?: System.currentTimeMillis()
        alarmText = record?.alarmText ?: record?.record
        alarmTime = alarmDateTime!! % 86400000
        alarmDate = alarmDateTime!! - alarmTime!!

        return binding.root
    }

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
                .setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)
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
            Log.e ("МОЁ", "btnSave")
        }

        buttonBinding.btnDel.setOnClickListener {
            Log.e ("МОЁ", "btnDel")
        }

        buttonBinding.btnCancel.setOnClickListener {
            Log.e ("МОЁ", "btnCancel")
        }


    }

    private fun dateVerification(): Boolean {
        if (alarmDateTime!! < System.currentTimeMillis() + 60000) {
            binding.tvDate.setBackgroundResource(R.drawable.field_red)
            binding.tvTime.setBackgroundResource(R.drawable.field_red)
            binding.tvAlarmErr.visibility = View.VISIBLE
            binding.tvNow.visibility = View.VISIBLE
            binding.tvNow.text = getString(R.string.now, DateFormat.format("dd.M.yy   HH:mm", System.currentTimeMillis()))
            return false
        } else {
            binding.tvDate.setBackgroundResource(R.drawable.field_blue)
            binding.tvTime.setBackgroundResource(R.drawable.field_blue)
            binding.tvAlarmErr.visibility = View.INVISIBLE
            binding.tvNow.visibility = View.INVISIBLE
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}