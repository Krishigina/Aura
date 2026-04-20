package com.aura.feature.journal.di

import com.aura.feature.journal.data.api.AuraSkinJournalApi
import com.aura.feature.journal.data.api.SkinJournalApi
import com.aura.feature.journal.data.repository.SkinJournalRepositoryImpl
import com.aura.feature.journal.domain.repository.SkinJournalRepository
import com.aura.feature.journal.presentation.SkinJournalViewModel
import org.koin.dsl.module

val skinJournalModule = module {
    single<SkinJournalApi> { AuraSkinJournalApi(get()) }
    single<SkinJournalRepository> { SkinJournalRepositoryImpl(get(), get()) }
    factory { SkinJournalViewModel(get()) }
}
