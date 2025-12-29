package com.a2t.myapplication.di

import com.a2t.myapplication.main.domain.api.MainInteractor
import com.a2t.myapplication.main.domain.impl.MainInteractorImpl
import com.a2t.myapplication.mediafile.domaim.api.MediaFileInteractor
import com.a2t.myapplication.mediafile.domaim.impl.MediaFileInteractorImpl
import org.koin.dsl.module

val interactorModule = module {

    single<MainInteractor> {
        MainInteractorImpl(mainRepository = get())
    }

    single<MediaFileInteractor> {
        MediaFileInteractorImpl(mediaFileConverter = get(), storagesRepository = get(), mediaFileDBRepositori = get())
    }
}