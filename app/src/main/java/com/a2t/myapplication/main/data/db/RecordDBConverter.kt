package com.a2t.myapplication.main.data.db

import com.a2t.myapplication.main.data.db.entity.ListRecordEntity
import com.a2t.myapplication.main.domain.model.ListRecord
import java.util.UUID

class RecordDBConverter {
    fun map(r: ListRecord): ListRecordEntity {
        return ListRecordEntity(
            r.id,
            r.idDir,
            r.isDir,
            r.npp,
            r.isChecked,
            r.record,
            r.note,
            textStyle = r.textColor * 100 + r.textStyle * 10 + r.textUnder,
            System.currentTimeMillis(),
            r.alarmTime,
            r.alarmText,
            r.alarmId?.toString(),
            r.isArchive,
            r.isDelete,
            r.mediaFile
        )
    }
    fun map(r: ListRecordEntity): ListRecord {
        return ListRecord(
            r.id,
            r.idDir,
            r.isDir,
            r.npp,
            r.isChecked,
            r.record,
            r.note,
            textColor = r.textStyle / 100,
            textStyle = (r.textStyle % 100) / 10,
            textUnder = r.textStyle % 10,
            r.lastEditTime,
            r.alarmTime,
            r.alarmText,
            r.alarmId?.let { UUID.fromString(it) },
            r.isArchive,
            r.isDelete,
            r.mediaFile,
            isFull = false,
            isAllCheck = false,
            isNew = false,
            startEdit = false,
            isEdit = false
        )
    }
}