package com.a2t.myapplication.main.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.a2t.myapplication.main.data.entity.ListRecordEntity

@Dao
interface MainRecordDao {

    // Добавление новой записи
    @Insert(entity = ListRecordEntity::class, onConflict = OnConflictStrategy.REPLACE)
    fun insertRecord(records: ListRecordEntity): Long

    // Обновление записи
    @Update(entity = ListRecordEntity::class, onConflict = OnConflictStrategy.REPLACE)
    fun updateRecord(record: ListRecordEntity)

    // Обновление записей
    @Update(entity = ListRecordEntity::class, onConflict = OnConflictStrategy.REPLACE)
    fun updateRecords(records: List<ListRecordEntity>)

    // Возвращает список записей для режимов NORMAL, MOVE, DELETE
    @Query("SELECT * FROM list_table WHERE idDir = :idDir AND isArchive = 0 AND isDelete = 0 ORDER BY npp ASC")
    fun getRecordsForNormalMoveDeleteModes(idDir: Long): List<ListRecordEntity>

    // Возвращает список записей для режимов NORMAL, MOVE, DELETE с сортировкой по isChecked
    @Query("SELECT * FROM list_table WHERE idDir = :idDir AND isArchive = 0 AND isDelete = 0 ORDER BY isChecked ASC, npp ASC")
    fun getRecordsForNormalMoveDeleteModesByCheck(idDir: Long): List<ListRecordEntity>

    // Возвращает список записей для режимов RESTORE(isDelete = 1), ARCHIVE(isDelete = 0)
    @Query("SELECT * FROM list_table WHERE idDir = :idDir AND isDelete = :isDelete ORDER BY npp ASC")
    fun getRecordsForRestoreArchiveModes(idDir: Long, isDelete: Int): List<ListRecordEntity>

    // Возвращает список записей для режимов RESTORE(isDelete = 1), ARCHIVE(isDelete = 0) с сортировкой по isChecked
    @Query("SELECT * FROM list_table WHERE idDir = :idDir AND isDelete = :isDelete ORDER BY isChecked ASC, npp ASC")
    fun getRecordsForRestoreArchiveModesByCheck(idDir: Long, isDelete: Int): List<ListRecordEntity>

    // Возвращает список имен папок с одним элементом - именем папки с id = idDir
    @Query("SELECT record FROM list_table WHERE id = :idDir")
    fun getNameDir(idDir: Long): LiveData<List<String>>

    // Возвращает список id родительских папок с одним элементом - id родительской папки для папки с id = idDir
    @Query("SELECT idDir FROM list_table WHERE id = :idDir")
    fun getParentDir(idDir: Long): LiveData<List<Long>>



    /*@Query("DELETE FROM favorite_table WHERE trackId = :trackId")
    fun deleteTrackById (trackId:Int)*/

    /*@Query("SELECT trackId FROM favorite_table")
    fun getTracksId(): List<Int>*/
}
// true = 1
// false = 0