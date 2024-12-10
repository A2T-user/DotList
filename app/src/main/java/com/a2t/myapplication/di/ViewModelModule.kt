package com.a2t.myapplication.di

import com.a2t.myapplication.main.presentation.MainViewModel
import com.a2t.myapplication.root.presentation.SharedViewModel
import com.a2t.myapplication.settings.presentation.SettingsViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {

    viewModel {
        MainViewModel(mainInteractor = get())
    }

    viewModel {
        SettingsViewModel(settingsInteractor = get())
    }

    viewModel { SharedViewModel() }
}