package com.aura.feature.chat.di

import com.aura.feature.chat.data.api.AuraChatApi
import com.aura.feature.chat.data.api.ChatApi
import com.aura.feature.chat.data.repository.ChatRepositoryImpl
import com.aura.feature.chat.data.repository.ChatSessionsRepositoryImpl
import com.aura.feature.chat.domain.repository.ChatRepository
import com.aura.feature.chat.domain.repository.ChatSessionsRepository
import com.aura.feature.chat.domain.usecase.GetChatSessionsUseCase
import com.aura.feature.chat.domain.usecase.LoadChatConversationUseCase
import com.aura.feature.chat.domain.usecase.SendChatMessageUseCase
import com.aura.feature.chat.domain.usecase.UploadChatAttachmentUseCase
import com.aura.feature.chat.presentation.ChatNavigationState
import com.aura.feature.chat.presentation.ChatSessionsViewModel
import com.aura.feature.chat.presentation.ChatViewModel
import org.koin.dsl.module

val chatModule = module {
    single { ChatNavigationState() }
    single<ChatApi> { AuraChatApi(get()) }
    single<ChatRepository> { ChatRepositoryImpl(get(), get()) }
    single<ChatSessionsRepository> { ChatSessionsRepositoryImpl(get(), get()) }
    factory { LoadChatConversationUseCase(get()) }
    factory { SendChatMessageUseCase(get()) }
    factory { UploadChatAttachmentUseCase(get()) }
    factory { GetChatSessionsUseCase(get()) }
    single { ChatViewModel(get(), get(), get()) }
    factory { ChatSessionsViewModel(get()) }
}
