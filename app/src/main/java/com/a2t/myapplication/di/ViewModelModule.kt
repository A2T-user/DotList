package com.a2t.myapplication.di

import com.a2t.myapplication.main.presentation.MainViewModel
import com.a2t.myapplication.mediafile.presentation.MediaFileViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule = module {

    viewModelOf(::MainViewModel)
    viewModelOf(::MediaFileViewModel)
}