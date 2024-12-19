package com.a2t.myapplication.main.data.db.dao

import androidx.room.Dao
import androidx.room.Query

@Dao
interface AlarmDao {
    // Обнуляем устаревшие Alarm-ы
    @Query("UPDATE list_table SET alarmTime = NULL, alarmText = NULL, alarmId = NULL WHERE alarmTime < :time")
    fun deleteOldAlarm (time: Long)
}