package com.a2t.myapplication.main.presentation.model

enum class OpenDirMode {
    NEW_DIR,            // Открытие при загрузке, смене режима
    CHILD_DIR,          // Переход в дочернюю папку
    PARENT_DIR          // Возврат в родительскую папку
}