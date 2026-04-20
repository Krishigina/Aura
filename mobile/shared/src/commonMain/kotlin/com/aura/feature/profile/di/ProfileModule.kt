package com.aura.feature.profile.di

import com.aura.feature.profile.data.api.AuraProfileApi
import com.aura.feature.profile.data.api.ProfileApi
import com.aura.feature.profile.data.repository.ProfileFavoritesRepositoryImpl
import com.aura.feature.profile.data.repository.ProfileNotificationsRepositoryImpl
import com.aura.feature.profile.data.repository.ProfileParametersRepositoryImpl
import com.aura.feature.profile.data.repository.ProfileRoutineRepositoryImpl
import com.aura.feature.profile.data.repository.ProfileSettingsRepositoryImpl
import com.aura.feature.profile.domain.repository.ProfileFavoritesRepository
import com.aura.feature.profile.domain.repository.ProfileNotificationsRepository
import com.aura.feature.profile.domain.repository.ProfileParametersRepository
import com.aura.feature.profile.domain.repository.ProfileRoutineRepository
import com.aura.feature.profile.domain.repository.ProfileSettingsRepository
import com.aura.feature.profile.domain.usecase.LoadProfileNotificationsUseCase
import com.aura.feature.profile.domain.usecase.LoadProfileRoutineUseCase
import com.aura.feature.profile.domain.usecase.RefreshSkinPassportUseCase
import com.aura.feature.profile.domain.usecase.SaveProfileNotificationsUseCase
import com.aura.feature.profile.domain.usecase.SaveProfileRoutineUseCase
import com.aura.feature.profile.domain.usecase.SearchRoutineProductsUseCase
import com.aura.feature.profile.presentation.favorites.ProfileFavoritesViewModel
import com.aura.feature.profile.presentation.notifications.ProfileNotificationsViewModel
import com.aura.feature.profile.presentation.parameters.ProfileParametersViewModel
import com.aura.feature.profile.presentation.routine.ProfileRoutineViewModel
import com.aura.feature.profile.presentation.settings.ProfileSettingsViewModel
import org.koin.dsl.module

val profileModule = module {
    single<ProfileApi> { AuraProfileApi(get(), get(), get(), get()) }
    single<ProfileNotificationsRepository> { ProfileNotificationsRepositoryImpl(get(), get()) }
    single<ProfileParametersRepository> { ProfileParametersRepositoryImpl(get(), get(), get()) }
    single<ProfileFavoritesRepository> { ProfileFavoritesRepositoryImpl(get(), get()) }
    single<ProfileRoutineRepository> { ProfileRoutineRepositoryImpl(get(), get()) }
    single<ProfileSettingsRepository> { ProfileSettingsRepositoryImpl(get(), get()) }
    factory { LoadProfileNotificationsUseCase(get()) }
    factory { LoadProfileRoutineUseCase(get()) }
    factory { SaveProfileNotificationsUseCase(get()) }
    factory { SaveProfileRoutineUseCase(get()) }
    factory { SearchRoutineProductsUseCase(get()) }
    factory { RefreshSkinPassportUseCase(get()) }
    factory { ProfileFavoritesViewModel(get()) }
    factory { ProfileNotificationsViewModel(get(), get()) }
    factory { ProfileParametersViewModel(get(), get()) }
    factory { ProfileRoutineViewModel(get(), get(), get()) }
    factory { ProfileSettingsViewModel(get()) }
}
