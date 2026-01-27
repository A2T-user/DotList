package com.a2t.myapplication.main.domain.impl

import com.a2t.myapplication.main.domain.api.GCInteractor
import com.a2t.myapplication.main.domain.api.GCRepository

class GCInteractorImpl(
    private val repository: GCRepository
): GCInteractor {
    override suspend fun run() {
        repository.run()
    }
}