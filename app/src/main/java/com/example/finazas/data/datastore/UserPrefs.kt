package com.example.finanzas.data.datastore


import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_prefs")

object UserPrefs {
    private val KEY_ONBOARD_DONE = booleanPreferencesKey("onboard_done")

    fun onboardDoneFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[KEY_ONBOARD_DONE] ?: false }

    suspend fun setOnboardDone(context: Context, done: Boolean) {
        context.dataStore.edit { it[KEY_ONBOARD_DONE] = done }
    }
}


