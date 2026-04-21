package com.aura.core.data.repository.passport

import com.aura.core.domain.repository.SkinPassportRepository

class MemorySkinPassportRepository : SkinPassportRepository {
    override fun answers(): Map<String, List<String>> {
        return SkinPassportManager.passport?.answers.orEmpty()
    }

    override fun save(answers: Map<String, List<String>>) {
        SkinPassportManager.save(answers)
    }

    override fun clear() {
        SkinPassportManager.clear()
    }

    override fun isCompleted(): Boolean {
        return SkinPassportManager.isCompleted()
    }
}
