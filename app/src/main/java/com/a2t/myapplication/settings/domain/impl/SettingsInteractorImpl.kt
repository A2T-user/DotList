package com.a2t.myapplication.settings.domain.impl

import com.a2t.myapplication.settings.domain.api.SettingsInteractor
import com.a2t.myapplication.settings.domain.api.SettingsRepository
import com.a2t.myapplication.settings.domain.model.AppSettings

class SettingsInteractorImpl(
    private val settingsRepository: SettingsRepository
): SettingsInteractor {

    override fun getSettings(): AppSettings {
        return  settingsRepository.getSettings()
    }

    override fun updateSettings(appSettings: AppSettings) {
        settingsRepository.updateSettings(appSettings)
    }
}