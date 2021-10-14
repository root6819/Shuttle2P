package com.simplecityapps.shuttle.mediaprovider.jellyfin.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.simplecityapps.shuttle.deviceinfo.DeviceInfo
import com.simplecityapps.shuttle.mediaprovider.jellyfin.AuthenticationManager
import com.simplecityapps.shuttle.mediaprovider.jellyfin.CredentialStore
import com.simplecityapps.shuttle.mediaprovider.jellyfin.JellyfinMediaProvider
import com.simplecityapps.shuttle.mediaprovider.jellyfin.http.service.ItemsService
import com.simplecityapps.shuttle.mediaprovider.jellyfin.http.service.UserService
import com.simplecityapps.shuttle.mediaprovider.jellyfin.preferences.JellyfinPreferenceManager
import com.simplecityapps.shuttle.preferences.di.PreferencesModule
import com.simplecityapps.shuttle.security.SecurityManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Named
import javax.inject.Singleton

@Module(includes = [PreferencesModule::class])
@InstallIn(SingletonComponent::class)
class MediaImportModule {

    @Provides
    @Singleton
    @Named("JellyfinDatastore")
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        ) {
            context.preferencesDataStoreFile("jellyfin")
        }
    }

    @Provides
    @Singleton
    fun provideJellyfinPreferenceManager(@Named("JellyfinDatastore") dataStore: DataStore<Preferences>, securityManager: SecurityManager): JellyfinPreferenceManager {
        return JellyfinPreferenceManager(dataStore = dataStore, securityManager = securityManager)
    }

    @Provides
    @Singleton
    fun provideHttpClient(): HttpClient {
        return HttpClient()
    }

    @Provides
    @Singleton
    fun provideItemsService(httpClient: HttpClient): ItemsService {
        return ItemsService(httpClient)
    }

    @Provides
    @Singleton
    fun provideUserService(httpClient: HttpClient): UserService {
        return UserService(httpClient)
    }

    @Provides
    @Singleton
    fun provideCredentialStore(preferenceManager: JellyfinPreferenceManager): CredentialStore {
        return CredentialStore(securePreferenceManager = preferenceManager)
    }

    @Provides
    @Singleton
    fun provideDeviceInfo(): DeviceInfo {
        return DeviceInfo()
    }

    @Provides
    @Singleton
    fun provideAuthenticationManager(userService: UserService, credentialStore: CredentialStore, deviceInfo: DeviceInfo): AuthenticationManager {
        return AuthenticationManager(userService = userService, credentialStore = credentialStore, deviceInfo = deviceInfo)
    }


    @Provides
    @Singleton
    fun provideMediaStoreMediaProvider(authenticationManager: AuthenticationManager, credentialStore: CredentialStore, itemsService: ItemsService): JellyfinMediaProvider {
        return JellyfinMediaProvider(authenticationManager = authenticationManager, credentialStore = credentialStore, itemsService = itemsService)
    }
}