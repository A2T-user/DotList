package com.a2t.myapplication.settings.domain.api

import com.a2t.myapplication.settings.domain.model.AppSettings

interface SettingsRepository {
    fun getSettings(): AppSettings
    fun updateSettings(appSettings: AppSettings)
}