package com.a2t.myapplication.di

import com.a2t.myapplication.main.domain.api.MainInteractor
import com.a2t.myapplication.main.domain.impl.MainInteractorImpl
import org.koin.dsl.module

val interactorModule = module {

    single<MainInteractor> {
        MainInteractorImpl(mainRepository = get())
    }
}