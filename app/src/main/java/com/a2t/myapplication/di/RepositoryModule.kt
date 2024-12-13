package com.a2t.myapplication.di

import com.a2t.myapplication.root.data.db.impl.SharedRepositoryImpl
import com.a2t.myapplication.root.domain.api.SharedRepository
import org.koin.dsl.module

val repositoryModule = module {

    single<SharedRepository> {
        SharedRepositoryImpl(appDatabase = get(), recordDBConverter = get())
    }
}