package com.a2t.myapplication.common.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.a2t.myapplication.main.data.dao.MainRecordDao
import com.a2t.myapplication.main.data.entity.ListRecordEntity

@Database(version = 1, entities = [ListRecordEntity::class], exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun mainRecordDao(): MainRecordDao
}