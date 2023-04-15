package com.h4pay.store.repository

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.h4pay.store.App
import com.h4pay.store.Prefs
import com.h4pay.store.repository.PrefsRepository.PreferenceKeys.TOKEN
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore by preferencesDataStore(name = "prefs")

class PrefsRepository(private val context: Context) {
    private object PreferenceKeys {
        val TOKEN = stringPreferencesKey("token")
    }

    suspend fun getSchoolToken(): Flow<String?> = context.dataStore.data.catch { exception ->
        if (exception is IOException) {
            Log.e("Error reading preferences: ", exception.toString())
            emit(emptyPreferences())
        } else {
            throw exception
        }
    }.map {
        mapSchoolPrefs(it)
    }

    suspend fun setSchoolToken(token: String?) = context.dataStore.edit { preferences ->
        preferences[TOKEN] = token ?: ""
    }

    suspend fun signOut() = context.dataStore.edit { preferences ->
        preferences[TOKEN] = ""
        App.token = null
    }

    private fun mapPrefs(prefs: Preferences): Prefs {
        val school = mapSchoolPrefs(prefs)
        return Prefs(school)
    }

    private fun mapSchoolPrefs(preferences: Preferences): String? {
        return preferences[TOKEN]
    }
}