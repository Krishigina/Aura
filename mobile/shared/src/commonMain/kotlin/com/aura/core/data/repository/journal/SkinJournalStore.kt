package com.aura.core.data.repository.journal

import com.aura.core.domain.model.SkinJournalResponse

object SkinJournalStore {
    var journal: SkinJournalResponse = SkinJournalResponse()
        private set

    fun save(value: SkinJournalResponse) {
        journal = value
    }

    fun clear() {
        journal = SkinJournalResponse()
    }
}
