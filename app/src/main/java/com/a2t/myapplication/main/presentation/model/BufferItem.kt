package com.a2t.myapplication.main.presentation.model

data class BufferItem(
    var id: Long,       // id записи
    var action: Int     // Действие с записью: 0-ничего, 1-перенести, 2-копировать, 3-удалить, 4-восстановить
)