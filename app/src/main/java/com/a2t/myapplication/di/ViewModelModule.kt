package com.a2t.myapplication.di

import com.a2t.myapplication.main.presentation.MainViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {

    viewModel { MainViewModel(mainInteractor = get()) }
}