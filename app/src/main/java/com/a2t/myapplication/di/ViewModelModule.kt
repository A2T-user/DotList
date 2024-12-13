package com.a2t.myapplication.di

import com.a2t.myapplication.root.presentation.SharedViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {

    viewModel { SharedViewModel(sharedInteractor = get()) }
}