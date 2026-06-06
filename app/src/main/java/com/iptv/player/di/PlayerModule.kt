package com.iptv.player.di

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PlayerModule {

    /** ExoPlayer reuses the app OkHttp client so it shares UA/timeouts with the API layer. */
    @OptIn(UnstableApi::class)
    @Provides
    @Singleton
    fun provideDataSourceFactory(
        @ApplicationContext context: Context,
        client: OkHttpClient,
    ): DataSource.Factory {
        val httpFactory = OkHttpDataSource.Factory(client)
            .setUserAgent("StreamDeckTV/0.1 (Android)")
        return DefaultDataSource.Factory(context, httpFactory)
    }
}
