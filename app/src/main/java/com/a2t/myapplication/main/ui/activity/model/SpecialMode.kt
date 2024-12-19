package com.a2t.myapplication.main.ui.activity.model

enum class SpecialMode: Special {
    NORMAL,
    MOVE,
    DELETE,
    RESTORE,
    ARCHIVE;

    override fun getModeName(): String =
        when(this) {
            NORMAL -> "NORMAL"
            MOVE -> "MOVE"
            DELETE -> "DELETE"
            RESTORE -> "RESTORE"
            ARCHIVE -> "ARCHIVE"
        }

    override fun getModeByName(name: String): SpecialMode =
        when(name) {
            "MOVE" -> MOVE
            "DELETE" -> DELETE
            "RESTORE" -> RESTORE
            "ARCHIVE" -> ARCHIVE
            else -> NORMAL
        }
}

interface Special {

    fun getModeName(): String
    fun getModeByName(name: String): SpecialMode
}
