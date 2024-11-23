package com.a2t.myapplication.settings.data.impl

import android.content.SharedPreferences
import androidx.core.content.edit
import com.a2t.myapplication.App
import com.a2t.myapplication.App.Companion.CROSSED_OUT_ON
import com.a2t.myapplication.App.Companion.EDIT_EMPTY_DIR
import com.a2t.myapplication.App.Companion.HINT_TOAST_ON
import com.a2t.myapplication.App.Companion.NOTIFICATION_ON
import com.a2t.myapplication.App.Companion.RESTORE_PERIOD
import com.a2t.myapplication.App.Companion.SORTING_CHECKS
import com.a2t.myapplication.App.Companion.TEXT_SIZE
import com.a2t.myapplication.settings.domain.api.SettingsRepository
import com.a2t.myapplication.settings.domain.model.AppSettings

class SettingsRepositoryImpl(
    private val sharedPrefs: SharedPreferences
): SettingsRepository {

    override fun getSettings(): AppSettings {
        return AppSettings(
            sharedPrefs.getString(App.STATE_THEME, App.SYSTEM),
            sharedPrefs.getInt(RESTORE_PERIOD, 3),
            sharedPrefs.getBoolean(EDIT_EMPTY_DIR, true),
            sharedPrefs.getBoolean(SORTING_CHECKS, true),
            sharedPrefs.getBoolean(CROSSED_OUT_ON, true),
            sharedPrefs.getBoolean(NOTIFICATION_ON, true),
            sharedPrefs.getBoolean(HINT_TOAST_ON, true),
            sharedPrefs.getFloat(TEXT_SIZE, 20f)
        )
    }

    override fun updateSettings(appSettings: AppSettings) {
        sharedPrefs.edit {
            putString(App.STATE_THEME, appSettings.stateTheme)
            putInt(RESTORE_PERIOD, appSettings.restorePeriod)
            putBoolean(EDIT_EMPTY_DIR, appSettings.editEmptyDir)
            putBoolean(SORTING_CHECKS, appSettings.sortingChecks)
            putBoolean(CROSSED_OUT_ON, appSettings.crossedOutOn)
            putBoolean(NOTIFICATION_ON, appSettings.notificationOn)
            putBoolean(HINT_TOAST_ON, appSettings.hintToastOn)
            putFloat(TEXT_SIZE, appSettings.textSize)
        }
    }

}