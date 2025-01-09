package com.example.matchmakingtest.di

import android.content.Context
import com.example.matchmakingtest.connection.WebRTCManager
import com.gobinda.connection.api.RemoteConnector
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class WebRtcModule {
//
//    @Provides
//    fun getWebRtcManager(@ApplicationContext context: Context): WebRTCManager {
//        return WebRTCManager(context)
//    }

    @Provides
    @Singleton
    fun getRemoteConnector(@ApplicationContext context: Context): RemoteConnector {
        return RemoteConnector(context)
    }
}