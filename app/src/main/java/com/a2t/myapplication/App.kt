package com.a2t.myapplication

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.room.Room
import com.a2t.myapplication.di.dataModule
import com.a2t.myapplication.di.interactorModule
import com.a2t.myapplication.di.repositoryModule
import com.a2t.myapplication.di.viewModelModule
import com.a2t.myapplication.main.data.db.AppDatabase
import com.a2t.myapplication.main.ui.fragments.models.AppSettings
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class App : Application() {

    companion object {
        const val LIGHT = "LIGHT"
        const val DARK = "DARK"
        const val SYSTEM = "SYSTEM"
        const val STATE_THEME = "STATE_THEME"
        const val RESTORE_PERIOD = "RESTORE_PERIOD"
        const val EDIT_EMPTY_DIR = "EDIT_EMPTY_DIR"
        const val SORTING_CHECKS = "SORTING_СHECKS"
        const val CROSSED_OUT_ON = "CROSSED_OUT_ON"
        const val NOTIFICATION_ON = "NOTIFICATION_ON"
        const val TEXT_SIZE = "TEXT_SIZE"
        lateinit var appSettings: AppSettings
        lateinit var appContext: Context
        var textSizeLiveData = MutableLiveData(20f)

        fun getTextSizeLiveData(): LiveData<Float> = textSizeLiveData
    }

    override fun onCreate() {
        super.onCreate()

        Room.databaseBuilder(applicationContext, AppDatabase::class.java, "database.db")
            .fallbackToDestructiveMigration()
            .build()

        getSettings()
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
        val pref = getSharedPreferences("list_preferences", Context.MODE_PRIVATE)
        appSettings = AppSettings(
            pref.getString(STATE_THEME, SYSTEM),
            pref.getInt(RESTORE_PERIOD, 3),
            pref.getBoolean(EDIT_EMPTY_DIR, true),
            pref.getBoolean(SORTING_CHECKS, true),
            pref.getBoolean(CROSSED_OUT_ON, true),
            pref.getBoolean(NOTIFICATION_ON, true),
            pref.getFloat(TEXT_SIZE, 20f)
        )
        textSizeLiveData.postValue(appSettings.textSize)
    }

    fun saveSettings () {
        val pref = getSharedPreferences("list_preferences", Context.MODE_PRIVATE)
        pref.edit {
            putString(STATE_THEME, appSettings.stateTheme)
            putInt(RESTORE_PERIOD, appSettings.restorePeriod)
            putBoolean(EDIT_EMPTY_DIR, appSettings.editEmptyDir)
            putBoolean(SORTING_CHECKS, appSettings.sortingChecks)
            putBoolean(CROSSED_OUT_ON, appSettings.crossedOutOn)
            putBoolean(NOTIFICATION_ON, appSettings.notificationOn)
            putFloat(TEXT_SIZE, appSettings.textSize)
        }
    }

    fun setTextSize (size: Float) {
        appSettings.textSize = size
        textSizeLiveData.postValue(size)
    }
}
