package com.a2t.myapplication.di

import com.a2t.myapplication.root.domain.api.SharedInteractor
import com.a2t.myapplication.root.domain.impl.SharedInteractorImpl
import org.koin.dsl.module

val interactorModule = module {

    single<SharedInteractor> {
        SharedInteractorImpl(mainRepository = get())
    }
}