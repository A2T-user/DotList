package com.a2t.myapplication.settings.presentation

import androidx.lifecycle.ViewModel
import com.a2t.myapplication.settings.domain.api.SettingsInteractor
import com.a2t.myapplication.settings.domain.model.AppSettings

class SettingsViewModel(
    private val settingsInteractor: SettingsInteractor,
) : ViewModel() {

    fun updateSettings(appSettings: AppSettings) {
        settingsInteractor.updateSettings(appSettings)
    }
}