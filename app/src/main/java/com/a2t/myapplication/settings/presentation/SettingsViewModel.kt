package com.a2t.myapplication.settings.presentation

import androidx.lifecycle.ViewModel
import com.a2t.myapplication.App
import com.a2t.myapplication.settings.domain.api.SettingsInteractor

class SettingsViewModel(
    private val settingsInteractor: SettingsInteractor,
) : ViewModel() {

    fun updateSettings() {
        settingsInteractor.updateSettings(App.appSettings)
    }
}