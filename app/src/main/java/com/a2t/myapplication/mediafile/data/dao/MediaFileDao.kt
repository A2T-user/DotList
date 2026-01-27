package com.a2t.myapplication.mediafile.data.dao

import androidx.room.Dao
import androidx.room.Query

@Dao
interface MediaFileDao {
    // Обновляем медиафайл
    @Query("UPDATE list_table SET mediaFile = :fileName, lastEditTime = :time WHERE id = :id")
    fun updateMediaFile (id: Long, fileName: String?, time: Long)

    // Обновляем поля Основное и примечание
    @Query("UPDATE list_table SET record = :record, note = :note, lastEditTime = :time WHERE id = :id")
    fun updateRecordAndNote (id: Long, record: String, note: String, time: Long)

}