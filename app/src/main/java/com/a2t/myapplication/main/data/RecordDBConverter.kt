package com.a2t.myapplication.main.data

import com.a2t.myapplication.main.data.entity.ListRecordEntity
import com.a2t.myapplication.main.domain.model.ListRecord

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
            r.isArchive,
            r.isDelete
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
            r.isArchive,
            r.isDelete,
            isFull = false,
            isAllCheck = false,
            isNew = false,
            startEdit = false,
            isEdit = false
        )
    }
}