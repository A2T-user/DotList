package com.a2t.myapplication.settings.ui.model

data class AppSettings(
    var stateTheme: String?,
    var restorePeriod: Int,
    var editEmptyDir: Boolean,
    var sortingChecks: Boolean,
    var crossedOutOn: Boolean,
    var notificationOn: Boolean,
    var hintToastOn: Boolean,
    var textSize: Float
)