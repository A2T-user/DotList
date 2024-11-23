package com.a2t.myapplication.main.domain.model

data class ListRecord(
    var id: Long,           // id записи
    var idDir: Long,        // id папки, в которой находися эта запись
    var isDir: Boolean,     // для строки = false, для папки = true
    var npp: Int,           // порядковый номер записи
    var isChecked: Boolean, // установлена ли галочка
    var record: String,     // Тест основного поля
    var note: String,       // Тест поля примечание, если пустое = ""
    var textColor: Int,     // Цвет шрита полей оснвное и примечание (0-регулярный, 1-красный, 2-желтый, 3-зеленый
    var textStyle: Int,     // Стиль шрита оснвного поля (0-регулярный, 1-болд, 2-италик, 3-болд/италик)
    var textUnder: Int,     // Подчеркивание шрита оснвного поля (0-не подчеркнутый, 1-подчеркнутый)
    var lastEditTime: Long, // Время последнего редактирования в системном формате (милиСек)
    var alarmTime: Long?,   // Время (в милисекундах) срабатывания напоминания
    var alarmText: String?, // Текст напоминания
    var isArchive: Boolean, // В архиве
    var isDelete: Boolean,  // Помечен, как удаленный
    // Параметры вычисляемые в репозитории
    var isFull: Boolean,    // для папки, если не пустая - true, пустая или строка - false
    var isAllCheck: Boolean,// для папки, если все записи в ней V - true, еcли нет - false
    // Параметры, задаваемые при добавлении новой записи
    var isNew: Boolean,     // Если запись новая - true, нет - false
    var startEdit: Boolean, // Включать ли режим редактирования при создании этой новой строки: да - true, нет - false
    // Становится true во время редактирования записи, в остальное время false
    var isEdit: Boolean     // Находится ли запись в режиме редактирования
)