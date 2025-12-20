package com.a2t.myapplication.main.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.a2t.myapplication.main.data.db.dao.AlarmDao
import com.a2t.myapplication.main.data.db.dao.MainRecordDao
import com.a2t.myapplication.main.data.db.entity.ListRecordEntity

// Миграция от версии 2 к версии 3: добавляем поле mediaFile
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE list_table ADD COLUMN mediaFile TEXT DEFAULT NULL")
    }
}

@Database(version = 3, entities = [ListRecordEntity::class], exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun mainRecordDao(): MainRecordDao

    abstract fun alarmDao(): AlarmDao
}