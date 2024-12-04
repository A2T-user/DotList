package com.a2t.myapplication.settings.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.a2t.myapplication.App
import com.a2t.myapplication.databinding.FragmentSettingsBinding
import com.a2t.myapplication.settings.domain.model.AppSettings
import com.a2t.myapplication.settings.presentation.SettingsViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

const val MIN_STORAGE_PERIOD_FOR_DELETED_RECORDS = 1
const val MAX_STORAGE_PERIOD_FOR_DELETED_RECORDS = 7

class SettingsFragment : Fragment() {

    private val settingsViewModel by viewModel<SettingsViewModel>()
    private lateinit var binding: FragmentSettingsBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSettingsBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
        binding.ivPlus.setOnClickListener {
            var period = App.appSettings.restorePeriod
            if (period < MAX_STORAGE_PERIOD_FOR_DELETED_RECORDS) {
                period++
                binding.tvRestorePeriod.text = period.toString()
                App.appSettings.restorePeriod = period
                settingsViewModel.updateSettings()
            }
        }

        binding.ivMinus.setOnClickListener {
            var period = App.appSettings.restorePeriod
            if (period > MIN_STORAGE_PERIOD_FOR_DELETED_RECORDS) {
                period--
                binding.tvRestorePeriod.text = period.toString()
                App.appSettings.restorePeriod = period
                settingsViewModel.updateSettings()
            }
        }
        // Редактирование
        binding.swEditEmptyDir.setOnCheckedChangeListener { _, checked ->
            App.appSettings.editEmptyDir = checked
            settingsViewModel.updateSettings()
        }
        // Сортировка
        binding.swSortingChecks.setOnCheckedChangeListener { _, checked ->
            App.appSettings.sortingChecks = checked
            settingsViewModel.updateSettings()
        }
        // Зачеркивание
        binding.swCrossingChecks.setOnCheckedChangeListener { _, checked ->
            App.appSettings.crossedOutOn = checked
            settingsViewModel.updateSettings()
        }
        // Напоминания
        binding.swNotificationOn.setOnCheckedChangeListener { _, checked ->
            App.appSettings.notificationOn = checked
            settingsViewModel.updateSettings()
        }
        // Сообщения
        binding.swShowHintToast .setOnCheckedChangeListener { _, checked ->
            App.appSettings.hintToastOn = checked
            settingsViewModel.updateSettings()
        }
    }

    private fun changeTheme() {
        installNullCheckTheme()
        settingsViewModel.updateSettings()
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
        binding.swEditEmptyDir.isChecked = newSettings.editEmptyDir             // Редактирование
        binding.swSortingChecks.isChecked = newSettings.sortingChecks           // Сортировка
        binding.swCrossingChecks.isChecked = newSettings.crossedOutOn           // Зачеркивание
        binding.swNotificationOn.isChecked = newSettings.notificationOn         // Напоминания
        binding.swShowHintToast.isChecked = newSettings.hintToastOn             // Сообщения
    }

    // Обнуляем чек во всех строках
    private fun installNullCheckTheme() {
        binding.ivLightTeme.alpha = 0f
        binding.ivDarkTeme.alpha = 0f
        binding.ivSystemTeme.alpha = 0f
    }
}