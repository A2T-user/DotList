package com.a2t.myapplication.root.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.a2t.myapplication.root.data.db.dao.SharedRecordDao
import com.a2t.myapplication.root.data.db.entity.ListRecordEntity

@Database(version = 1, entities = [ListRecordEntity::class], exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun mainRecordDao(): SharedRecordDao
}