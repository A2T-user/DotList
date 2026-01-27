package com.a2t.myapplication.di

import com.a2t.myapplication.main.data.db.impl.GCRepositoryImpl
import com.a2t.myapplication.main.data.db.impl.MainRepositoryImpl
import com.a2t.myapplication.main.domain.api.GCRepository
import com.a2t.myapplication.main.domain.api.MainRepository
import com.a2t.myapplication.mediafile.data.impl.MediaFileDBRepositoriImpl
import com.a2t.myapplication.mediafile.data.impl.StoragesRepositoryImpl
import com.a2t.myapplication.mediafile.domaim.api.MediaFileDBRepositori
import com.a2t.myapplication.mediafile.domaim.api.StoragesRepository
import org.koin.dsl.module

val repositoryModule = module {

    single<MainRepository> {
        MainRepositoryImpl(appDatabase = get(), recordDBConverter = get())
    }

    single<MediaFileDBRepositori> {
        MediaFileDBRepositoriImpl(appDatabase = get())
    }

    single<StoragesRepository> {
        StoragesRepositoryImpl()
    }

    single<GCRepository> {
        GCRepositoryImpl(appDatabase = get())
    }
}