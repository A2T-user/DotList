package com.a2t.myapplication.common

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.room.Room
import com.a2t.myapplication.common.model.AppSettings
import com.a2t.myapplication.di.dataModule
import com.a2t.myapplication.di.interactorModule
import com.a2t.myapplication.di.repositoryModule
import com.a2t.myapplication.di.viewModelModule
import com.a2t.myapplication.main.data.db.AppDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class App : Application() {
    private lateinit var  pref: SharedPreferences

    companion object {
        const val LIGHT = "LIGHT"
        const val DARK = "DARK"
        const val SYSTEM = "SYSTEM"
        const val LAUNCH_COUNTER = "LAUNCH_COUNTER"
        const val STATE_THEME = "STATE_THEME"
        const val RESTORE_PERIOD = "RESTORE_PERIOD"
        const val EDIT_EMPTY_DIR = "EDIT_EMPTY_DIR"
        const val SORTING_CHECKS = "SORTING_СHECKS"
        const val CROSSED_OUT_ON = "CROSSED_OUT_ON"
        const val NOTIFICATION_ON = "NOTIFICATION_ON"
        const val LEFT_HAND_CONTROL = "LEFT_HAND_CONTROL"
        const val TEXT_SIZE = "TEXT_SIZE"
        lateinit var appSettings: AppSettings
        lateinit var appContext: Context
        var textSizeLiveData = MutableLiveData(20f)
        fun getTextSizeLiveData(): LiveData<Float> = textSizeLiveData
    }

    override fun onCreate() {
        super.onCreate()

        pref = getSharedPreferences("list_preferences", MODE_PRIVATE)
        getSettings()
        appSettings.launchCounter++
        pref.edit { putInt(LAUNCH_COUNTER, appSettings.launchCounter) }
        switchTheme()
        startKoin {
            androidContext(this@App)
            modules(dataModule, repositoryModule, interactorModule, viewModelModule)
        }
        appContext = this@App
    }

    fun switchTheme() {
        AppCompatDelegate.setDefaultNightMode(
            when (appSettings.stateTheme) {
                DARK -> AppCompatDelegate.MODE_NIGHT_YES
                LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        )
    }

    private fun getSettings () {
        appSettings = AppSettings(
            pref.getInt(LAUNCH_COUNTER, 0),
            pref.getString(STATE_THEME, SYSTEM),
            pref.getInt(RESTORE_PERIOD, 3),
            pref.getBoolean(EDIT_EMPTY_DIR, true),
            pref.getBoolean(SORTING_CHECKS, true),
            pref.getBoolean(CROSSED_OUT_ON, true),
            pref.getBoolean(NOTIFICATION_ON, true),
            pref.getFloat(TEXT_SIZE, 20f),
            pref.getBoolean(LEFT_HAND_CONTROL, false)
        )
        textSizeLiveData.postValue(appSettings.textSize)
    }

    fun saveSettings () {
        pref.edit {
            putInt(LAUNCH_COUNTER, appSettings.launchCounter)
            putString(STATE_THEME, appSettings.stateTheme)
            putInt(RESTORE_PERIOD, appSettings.restorePeriod)
            putBoolean(EDIT_EMPTY_DIR, appSettings.editEmptyDir)
            putBoolean(SORTING_CHECKS, appSettings.sortingChecks)
            putBoolean(CROSSED_OUT_ON, appSettings.crossedOutOn)
            putBoolean(NOTIFICATION_ON, appSettings.notificationOn)
            putFloat(TEXT_SIZE, appSettings.textSize)
            putBoolean(LEFT_HAND_CONTROL, appSettings.isLeftHandControl)
        }
    }

    fun setTextSize (size: Float) {
        textSizeLiveData.postValue(size)
        appSettings.textSize = size
    }

    fun saveTextSize () {
        pref.edit { putFloat(TEXT_SIZE, appSettings.textSize) }
    }
}