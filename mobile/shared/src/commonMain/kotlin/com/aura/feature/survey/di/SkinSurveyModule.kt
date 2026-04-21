package com.aura.feature.survey.di

import com.aura.feature.survey.data.api.AuraSkinSurveyApi
import com.aura.feature.survey.data.api.SkinSurveyApi
import com.aura.feature.survey.data.repository.SkinSurveyRepositoryImpl
import com.aura.feature.survey.domain.repository.SkinSurveyRepository
import com.aura.feature.survey.presentation.SkinSurveyViewModel
import org.koin.dsl.module

val skinSurveyModule = module {
    single<SkinSurveyApi> { AuraSkinSurveyApi(get(), get(), get()) }
    single<SkinSurveyRepository> { SkinSurveyRepositoryImpl(get(), get(), get()) }
    factory { SkinSurveyViewModel(get()) }
}
