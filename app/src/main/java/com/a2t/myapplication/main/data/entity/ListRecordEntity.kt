package com.a2t.myapplication.main.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "list_table")
class ListRecordEntity (
    @PrimaryKey(autoGenerate = true)
    val id: Long,           // id записи
    val idDir: Long,        // id папки, в которой находися эта запись
    val isDir: Boolean,     // для строки = false, для папки = true
    val npp: Int,           // порядковый номер записи
    val isChecked: Boolean, // установлена ли галочка
    val record: String,     // Тест основного поля
    val note: String,       // Тест поля примечание, если пустое = ""
    val textStyle: Int,     // Стильотображения текстовых полей = цвет*100 + силь*10 + подчеркивание
    val lastEditTime: Long, // Время последнего редактирования в системном формате (милиСек)
    val alarmTime: Long?,   // Время (в милисекундах) срабатывания напоминания
    val alarmText: String?, // Текст напоминания
    val isArchive: Boolean, // В архиве
    val isDelete: Boolean    // Помечен, как удаленный
)