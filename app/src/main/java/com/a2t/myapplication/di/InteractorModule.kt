package com.a2t.myapplication.di

import com.a2t.myapplication.main.domain.api.GCInteractor
import com.a2t.myapplication.main.domain.api.MainInteractor
import com.a2t.myapplication.main.domain.impl.GCInteractorImpl
import com.a2t.myapplication.main.domain.impl.MainInteractorImpl
import com.a2t.myapplication.mediafile.domaim.api.MediaFileInteractor
import com.a2t.myapplication.mediafile.domaim.api.MediaViewerInteractor
import com.a2t.myapplication.mediafile.domaim.impl.MediaFileInteractorImpl
import com.a2t.myapplication.mediafile.domaim.impl.MediaViewerInteractorImpl
import org.koin.dsl.module

val interactorModule = module {

    single<MainInteractor> {
        MainInteractorImpl(mainRepository = get())
    }

    single<GCInteractor> {
        GCInteractorImpl(repository = get())
    }

    single<MediaFileInteractor> {
        MediaFileInteractorImpl(mediaFileConverter = get(), storagesRepository = get(), mediaFileDBRepositori = get())
    }

    single<MediaViewerInteractor> {
        MediaViewerInteractorImpl(mediaFileDBRepositori = get())
    }


}