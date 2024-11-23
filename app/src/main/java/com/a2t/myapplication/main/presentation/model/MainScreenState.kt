package com.a2t.myapplication.main.presentation.model

import com.a2t.myapplication.main.domain.model.ListRecord

data class MainScreenState(
    var specialMode: SpecialMode,
    var records: List<ListRecord>,
    var nameDir: String
)