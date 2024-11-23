package com.a2t.myapplication.main.data.dao

import androidx.room.Dao
import androidx.room.Query
import com.a2t.myapplication.main.data.entity.ListRecordEntity

@Dao
interface MainRecordDao {

    /*@Insert(entity = ListRecordEntity::class, onConflict = OnConflictStrategy.REPLACE)
    fun insertRecord(record: ListRecordEntity): LiveData<Long>

    @Update(entity = ListRecordEntity::class, onConflict = OnConflictStrategy.REPLACE)
    fun updateRecord(record: ListRecordEntity)*/


    @Query("SELECT * FROM list_table WHERE idDir = :idDir AND isArchive = 0 AND isDelete = 0 ORDER BY npp ASC")
    suspend fun getRecordsNormalMode(idDir: Long): List<ListRecordEntity>

    @Query("SELECT * FROM list_table WHERE idDir = :idDir AND isArchive = 0 AND isDelete = 0 ORDER BY isChecked ASC, npp ASC")
    suspend fun getRecordsNormalModeSortedByCheck(idDir: Long): List<ListRecordEntity>

    @Query("SELECT * FROM list_table WHERE idDir = :idDir AND isDelete = 0 ORDER BY npp ASC")
    suspend fun getRecordsArchiveMode(idDir: Long): List<ListRecordEntity>

    @Query("SELECT * FROM list_table WHERE idDir = :idDir AND isDelete = 1 ORDER BY npp ASC")
    suspend fun getRecordsRestoreMode(idDir: Long): List<ListRecordEntity>

    /*@Query("SELECT record FROM list_table WHERE id = :idDir")
    fun getNameDir(idDir: Long): LiveData<List<String>>

    @Query("SELECT id, record FROM list_table WHERE idDir = :idDir")
    fun getParentDir(idDir: Long): LiveData<List<Pair<Long, String>>>*/

    /*@Query("DELETE FROM favorite_table WHERE trackId = :trackId")
    fun deleteTrackById (trackId:Int)*/

    /*@Query("SELECT trackId FROM favorite_table")
    fun getTracksId(): List<Int>*/
}