package com.a2t.myapplication.common.model

data class AppSettings(
    var launchCounter: Int,
    var stateTheme: String?,
    var restorePeriod: Int,
    var editEmptyDir: Boolean,
    var sortingChecks: Boolean,
    var crossedOutOn: Boolean,
    var notificationOn: Boolean,
    var textSize: Float,
    var isLeftHandControl: Boolean
)