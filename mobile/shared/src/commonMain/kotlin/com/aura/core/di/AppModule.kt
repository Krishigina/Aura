package com.aura.core.di

import com.aura.core.data.api.AuraNetworkClients
import com.aura.core.data.api.client.AuthNetworkClient
import com.aura.core.data.api.client.ChatNetworkClient
import com.aura.core.data.api.client.GeneralContentNetworkClient
import com.aura.core.data.api.client.ProductNetworkClient
import com.aura.core.data.api.client.ProfileNetworkClient
import com.aura.core.data.api.client.RecommendationsNetworkClient
import com.aura.core.data.api.client.SkinJournalNetworkClient
import com.aura.core.data.repository.passport.MemorySkinPassportRepository
import com.aura.core.data.repository.session.TokenSessionRepository
import com.aura.core.domain.repository.SessionRepository
import com.aura.core.domain.repository.SkinPassportRepository
import com.aura.core.platform.defaultApiBaseUrl
import com.aura.feature.auth.di.authModule
import com.aura.feature.catalog.di.catalogModule
import com.aura.feature.chat.di.chatModule
import com.aura.feature.diagnostics.di.diagnosticsModule
import com.aura.feature.home.di.homeModule
import com.aura.feature.journal.di.skinJournalModule
import com.aura.feature.product.di.productModule
import com.aura.feature.profile.di.profileModule
import com.aura.feature.recommendations.di.recommendationsModule
import com.aura.feature.survey.di.skinSurveyModule
import org.koin.dsl.module

val appModule = module {
    single { AuraNetworkClients(defaultApiBaseUrl()) }
    single<AuthNetworkClient> { get<AuraNetworkClients>().authNetworkClient }
    single<ProductNetworkClient> { get<AuraNetworkClients>().productNetworkClient }
    single<ProfileNetworkClient> { get<AuraNetworkClients>().profileNetworkClient }
    single<SkinJournalNetworkClient> { get<AuraNetworkClients>().skinJournalNetworkClient }
    single<ChatNetworkClient> { get<AuraNetworkClients>().chatNetworkClient }
    single<GeneralContentNetworkClient> { get<AuraNetworkClients>().generalContentNetworkClient }
    single<RecommendationsNetworkClient> { get<AuraNetworkClients>().recommendationsNetworkClient }
    single<SessionRepository> { TokenSessionRepository() }
    single<SkinPassportRepository> { MemorySkinPassportRepository() }
}

val appModules = listOf(appModule, authModule, catalogModule, chatModule, diagnosticsModule, homeModule, productModule, profileModule, recommendationsModule, skinJournalModule, skinSurveyModule)
