package com.a2t.myapplication.di

import android.content.Context
import androidx.room.Room
import com.a2t.myapplication.root.data.db.AppDatabase
import com.a2t.myapplication.root.data.db.RecordDBConverter
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val dataModule = module {

    single {
        androidContext()
            .getSharedPreferences("list_preferences", Context.MODE_PRIVATE)
    }

    single {
        Room.databaseBuilder(androidContext(), AppDatabase::class.java, "database.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    factory { RecordDBConverter() }
}