package com.a2t.myapplication.main.ui.fragments.models

data class AppSettings(
    var stateTheme: String?,
    var restorePeriod: Int,
    var editEmptyDir: Boolean,
    var sortingChecks: Boolean,
    var crossedOutOn: Boolean,
    var notificationOn: Boolean,
    var textSize: Float
)