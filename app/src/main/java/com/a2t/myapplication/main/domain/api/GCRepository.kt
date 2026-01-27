package com.a2t.myapplication.main.domain.api

interface GCRepository {
    suspend fun run()
}