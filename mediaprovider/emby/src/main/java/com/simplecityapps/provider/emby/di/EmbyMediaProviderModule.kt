package com.simplecityapps.provider.emby.di

import android.content.Context
import androidx.core.content.getSystemService
import com.simplecityapps.networking.retrofit.NetworkResultAdapterFactory
import com.simplecityapps.provider.emby.CredentialStore
import com.simplecityapps.provider.emby.EmbyAuthenticationManager
import com.simplecityapps.provider.emby.EmbyMediaProvider
import com.simplecityapps.provider.emby.http.ItemsService
import com.simplecityapps.provider.emby.http.UserService
import com.simplecityapps.shuttle.dagger.AppScope
import com.simplecityapps.shuttle.persistence.SecurePreferenceManager
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.create

@Module
open class EmbyMediaProviderModule {

    @Provides
    @AppScope
    fun provideRetrofit(context: Context, okHttpClient: OkHttpClient, moshi: Moshi): Retrofit {
        return Retrofit.Builder()
            .baseUrl("http://localhgst/") // unused
            .addCallAdapterFactory(NetworkResultAdapterFactory(context.getSystemService()))
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(okHttpClient)
            .build()
    }

    @Provides
    @AppScope
    fun provideUserService(retrofit: Retrofit): UserService {
        return retrofit.create()
    }

    @Provides
    @AppScope
    fun provideItemsService(retrofit: Retrofit): ItemsService {
        return retrofit.create()
    }

    @Provides
    @AppScope
    fun provideCredentialStore(securePreferenceManager: SecurePreferenceManager): CredentialStore {
        return CredentialStore(securePreferenceManager)
    }

    @Provides
    @AppScope
    fun provideEmbyAuthenticationManager(userService: UserService, credentialStore: CredentialStore): EmbyAuthenticationManager {
        return EmbyAuthenticationManager(userService, credentialStore)
    }

    @Provides
    @AppScope
    fun provideEmbyMediaProvider(authenticationManager: EmbyAuthenticationManager, itemsService: ItemsService): EmbyMediaProvider {
        return EmbyMediaProvider(authenticationManager, itemsService)
    }
}