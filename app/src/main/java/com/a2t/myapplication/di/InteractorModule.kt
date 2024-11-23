package com.a2t.myapplication.di

import com.a2t.myapplication.main.domain.api.MainInteractor
import com.a2t.myapplication.main.domain.impl.MainInteractorImpl
import com.a2t.myapplication.settings.domain.api.SettingsInteractor
import com.a2t.myapplication.settings.domain.impl.SettingsInteractorImpl
import org.koin.dsl.module

val interactorModule = module {

    single<SettingsInteractor> {
        SettingsInteractorImpl(settingsRepository = get())
    }

    single<MainInteractor> {
        MainInteractorImpl(mainRepository = get())
    }
}