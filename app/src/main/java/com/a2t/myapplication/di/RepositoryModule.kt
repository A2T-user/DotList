package com.a2t.myapplication.di

import com.a2t.myapplication.main.data.impl.MainRepositoryImpl
import com.a2t.myapplication.main.domain.api.MainRepository
import com.a2t.myapplication.settings.data.impl.SettingsRepositoryImpl
import com.a2t.myapplication.settings.domain.api.SettingsRepository
import org.koin.dsl.module

val repositoryModule = module {

    single<SettingsRepository> {
        SettingsRepositoryImpl(sharedPrefs = get())
    }

    single<MainRepository> {
        MainRepositoryImpl(appDatabase = get(), recordDBConverter = get ())
    }
}