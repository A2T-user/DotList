package com.a2t.myapplication.main.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.a2t.myapplication.main.data.db.dao.AlarmDao
import com.a2t.myapplication.main.data.db.dao.MainRecordDao
import com.a2t.myapplication.main.data.db.entity.ListRecordEntity

@Database(version = 2, entities = [ListRecordEntity::class], exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun mainRecordDao(): MainRecordDao

    abstract fun alarmDao(): AlarmDao
}