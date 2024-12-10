package com.a2t.myapplication.di

import com.a2t.myapplication.root.domain.api.SharedInteractor
import com.a2t.myapplication.root.domain.impl.SharedInteractorImpl
import com.a2t.myapplication.settings.domain.api.SettingsInteractor
import com.a2t.myapplication.settings.domain.impl.SettingsInteractorImpl
import org.koin.dsl.module

val interactorModule = module {

    single<SettingsInteractor> {
        SettingsInteractorImpl(settingsRepository = get())
    }

    single<SharedInteractor> {
        SharedInteractorImpl(mainRepository = get())
    }
}