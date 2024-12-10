package com.a2t.myapplication.di

import com.a2t.myapplication.root.data.db.impl.SharedRepositoryImpl
import com.a2t.myapplication.root.domain.api.SharedRepository
import com.a2t.myapplication.settings.data.impl.SettingsRepositoryImpl
import com.a2t.myapplication.settings.domain.api.SettingsRepository
import org.koin.dsl.module

val repositoryModule = module {

    single<SettingsRepository> {
        SettingsRepositoryImpl(sharedPrefs = get())
    }

    single<SharedRepository> {
        SharedRepositoryImpl(appDatabase = get(), recordDBConverter = get())
    }
}