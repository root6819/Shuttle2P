package com.simplecityapps.shuttle.mediaimport

import com.simplecityapps.shuttle.model.MediaProviderType

interface MediaProviderFactory {
    fun getMediaProviders(mediaProviderTypes: List<MediaProviderType>): List<MediaProvider>
}