package com.a2t.myapplication.mediafile.data.dao

import androidx.room.Dao
import androidx.room.Query

@Dao
interface MediaFileDao {
    // Обнуляем устаревшие Alarm-ы
    @Query("UPDATE list_table SET mediaFile = :fileName WHERE id = :id")
    fun addMediaFile (id: Long, fileName: String)

    // Обнуляем устаревшие Alarm-ы
    @Query("UPDATE list_table SET mediaFile = NULL WHERE id = :id")
    fun deleteMediaFile (id: Long)
}