package com.a2t.myapplication.main.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.a2t.myapplication.main.data.db.entity.ListRecordEntity

@Dao
interface MainRecordDao {

    // Добавление новой записи
    @Insert(entity = ListRecordEntity::class, onConflict = OnConflictStrategy.REPLACE)
    fun insertRecord(record: ListRecordEntity): Long

    // Добавление новых записей
    @Insert(entity = ListRecordEntity::class, onConflict = OnConflictStrategy.REPLACE)
    fun insertRecords(records: List<ListRecordEntity>)

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
    fun getNameDir(idDir: Long): List<String>

    // Возвращает список id родительских папок с одним элементом - id родительской папки для папки с id = idDir
    @Query("SELECT idDir FROM list_table WHERE id = :idDir")
    fun getParentDirId(idDir: Long): List<Long>

    // Возвращает список подчиненных записей для удаления
    @Query("SELECT * FROM list_table WHERE idDir = :idDir AND isDelete = 0")
    fun selectionSubordinateRecordsToDelete(idDir: Long): List<ListRecordEntity>

    // Возвращает список подчиненных записей для восстановления
    @Query("SELECT * FROM list_table WHERE idDir = :idDir AND isDelete = 1")
    fun selectionSubordinateRecordsToRestore(idDir: Long): List<ListRecordEntity>

    // Удаление записей с истекшим сроком хранения
    @Query("DELETE FROM list_table WHERE isDelete = 1 AND lastEditTime < :time")
    fun deletingExpiredRecords(time: Long)
}