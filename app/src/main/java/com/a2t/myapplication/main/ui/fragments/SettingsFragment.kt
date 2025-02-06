package com.a2t.myapplication.main.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import com.a2t.myapplication.App
import com.a2t.myapplication.databinding.FragmentSettingsBinding
import com.a2t.myapplication.main.ui.activity.MainActivity
import com.a2t.myapplication.main.ui.fragments.models.AppSettings

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var app: App                 //= App.appContext as App
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        app = requireContext().applicationContext as App

        installNewSettings(App.appSettings)

        // Обработка нажатий выбора темы
        binding.lightTeme.setOnClickListener {
            App.appSettings.stateTheme = App.LIGHT
            changeTheme()
            binding.ivLightTeme.alpha = 1f

        }
        binding.darkTeme.setOnClickListener {
            App.appSettings.stateTheme = App.DARK
            changeTheme()
            binding.ivDarkTeme.alpha = 1f
        }
        binding.systemTeme.setOnClickListener {
            App.appSettings.stateTheme = App.SYSTEM
            changeTheme()
            binding.ivSystemTeme.alpha = 1f
        }
        // Период восстановления
        binding.seekBarRestorePeriod.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser:  Boolean) {
                val period = progress + 1
                binding.tvRestorePeriod.text = period.toString()
                App.appSettings.restorePeriod = period
                app.saveSettings()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        // Редактирование
        binding.swEditEmptyDir.setOnCheckedChangeListener { _, checked ->
            App.appSettings.editEmptyDir = checked
            app.saveSettings() // Сохраняем параметры
        }
        // Размер шрифта
        binding.seekBarTextSize.progress = App.appSettings.textSize.toInt()
        App.getTextSizeLiveData().observe(viewLifecycleOwner) { size ->
            binding.tvTextSize.textSize = size
            binding.tvTextSizeNumber.text = size.toString()
        }
        binding.seekBarTextSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser:  Boolean) {
                app.setTextSize(progress.toFloat())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        // Сортировка
        binding.swSortingChecks.setOnCheckedChangeListener { _, checked ->
            App.appSettings.sortingChecks = checked
            app.saveSettings() // Сохраняем параметры
        }
        // Зачеркивание
        binding.swCrossingChecks.setOnCheckedChangeListener { _, checked ->
            App.appSettings.crossedOutOn = checked
            app.saveSettings() // Сохраняем параметры
        }
        // Напоминания
        binding.swNotificationOn.setOnCheckedChangeListener { _, checked ->
            App.appSettings.notificationOn = checked
            app.saveSettings() // Сохраняем параметры
        }
        // Выбор ориентации боковой панели
        // правая
        binding.rightHand.setOnClickListener {
            binding.rightHand.alpha = 1.0f
            binding.leftHand.alpha = 0.3f
            App.appSettings.isLeftHandControl = false
            app.saveSettings()
            (requireActivity() as MainActivity).installHandControl()
        }
        // левая
        binding.leftHand.setOnClickListener {
            binding.leftHand.alpha = 1.0f
            binding.rightHand.alpha = 0.3f
            App.appSettings.isLeftHandControl = true
            app.saveSettings()
            (requireActivity() as MainActivity).installHandControl()
        }
    }

    private fun changeTheme() {
        installNullCheckTheme()
        app.saveSettings() // Сохраняем параметры
        (requireContext().applicationContext as App).switchTheme()
    }

    // Установка новых параметров
    private fun installNewSettings(newSettings: AppSettings) {
        // Тема
        val newTheme = newSettings.stateTheme
        installNullCheckTheme()
        when(newTheme) {
            App.LIGHT -> binding.ivLightTeme.alpha = 1f
            App.DARK -> binding.ivDarkTeme.alpha = 1f
            else -> binding.ivSystemTeme.alpha = 1f
        }
        binding.tvRestorePeriod.text = newSettings.restorePeriod.toString()     // Период восстановления
        binding.seekBarRestorePeriod.progress = newSettings.restorePeriod - 1
        binding.swEditEmptyDir.isChecked = newSettings.editEmptyDir             // Редактирование
        binding.swSortingChecks.isChecked = newSettings.sortingChecks           // Сортировка
        binding.swCrossingChecks.isChecked = newSettings.crossedOutOn           // Зачеркивание
        binding.swNotificationOn.isChecked = newSettings.notificationOn         // Напоминания
        if (newSettings.isLeftHandControl) {
            binding.leftHand.alpha = 1.0f
            binding.rightHand.alpha = 0.3f
        } else {
            binding.leftHand.alpha = 0.3f
            binding.rightHand.alpha = 1.0f
        }
    }

    // Обнуляем чек во всех строках
    private fun installNullCheckTheme() {
        binding.ivLightTeme.alpha = 0.3f
        binding.ivDarkTeme.alpha = 0.3f
        binding.ivSystemTeme.alpha = 0.3f
    }

    override fun onStart() {
        super.onStart()
        (requireActivity() as MainActivity).mainBackPressedCallback.isEnabled = false
    }

    override fun onResume() {
        super.onResume()
        view?.apply {
            isFocusable = true
            isFocusableInTouchMode = true
            requestFocus()
            setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    parentFragmentManager.beginTransaction().remove(this@SettingsFragment).commitAllowingStateLoss() // Закрытие фрагмента
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        (requireActivity() as MainActivity).mainBackPressedCallback.isEnabled = true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}