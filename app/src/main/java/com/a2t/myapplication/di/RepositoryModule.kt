package com.a2t.myapplication.di

import com.a2t.myapplication.main.data.db.impl.MainRepositoryImpl
import com.a2t.myapplication.main.domain.api.MainRepository
import org.koin.dsl.module

val repositoryModule = module {

    single<MainRepository> {
        MainRepositoryImpl(appDatabase = get(), recordDBConverter = get())
    }
}