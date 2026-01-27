package com.a2t.myapplication.main.data.db.dao

import androidx.room.Dao
import androidx.room.Query

@Dao
interface ListItemDao {
    @Query("SELECT mediaFile FROM list_table WHERE mediaFile IS NOT NULL GROUP BY mediaFile")
    suspend fun getDistinctMediaFiles(): List<String>
}