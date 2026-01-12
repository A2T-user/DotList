package com.a2t.myapplication.di

import android.content.Context
import androidx.room.Room
import com.a2t.myapplication.common.data.AppDatabase
import com.a2t.myapplication.common.data.MIGRATION_2_3
import com.a2t.myapplication.main.data.db.RecordDBConverter
import com.a2t.myapplication.mediafile.data.MediaFileConverter
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val dataModule = module {

    single {
        androidContext()
            .getSharedPreferences("list_preferences", Context.MODE_PRIVATE)
    }

    single {
        Room.databaseBuilder(androidContext(), AppDatabase::class.java, "database.db")
            .addMigrations(MIGRATION_2_3)
            .build()
    }

    factory { RecordDBConverter() }
    factory { MediaFileConverter() }
}