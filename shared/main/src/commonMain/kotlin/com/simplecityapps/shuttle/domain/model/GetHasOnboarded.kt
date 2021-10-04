package com.simplecityapps.shuttle.domain.model

import com.simplecityapps.shuttle.inject.Inject
import com.simplecityapps.shuttle.preferences.GeneralPreferenceManager
import kotlinx.coroutines.flow.Flow

class GetHasOnboarded @Inject constructor(
    private val preferenceManager: GeneralPreferenceManager
) {
    operator fun invoke(): Flow<Boolean> = preferenceManager.getHasOnboarded()
}