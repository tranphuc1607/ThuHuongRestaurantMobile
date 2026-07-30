package com.example.thuhuong_restaurant.core.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.sessionDataStore by preferencesDataStore(name = "thu_huong_session")

data class Session(
    val accessToken: String?,
    val username: String?,
    val role: String?,
)

/**
 * Persists the access token + minimal user identity. Mirrors AuthContext (front-end web):
 * token in storage, role/username re-derived from GET /users/me on startup.
 */
@Singleton
class TokenManager @Inject constructor(@ApplicationContext context: Context) {

    private val dataStore = context.sessionDataStore

    private object Keys {
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val USERNAME = stringPreferencesKey("username")
        val ROLE = stringPreferencesKey("role")
    }

    val session: Flow<Session> = dataStore.data.map { prefs ->
        Session(
            accessToken = prefs[Keys.ACCESS_TOKEN],
            username = prefs[Keys.USERNAME],
            role = prefs[Keys.ROLE],
        )
    }

    suspend fun currentAccessToken(): String? = dataStore.data.map { it[Keys.ACCESS_TOKEN] }.first()

    suspend fun saveSession(accessToken: String, username: String, role: String) {
        dataStore.edit { prefs ->
            prefs[Keys.ACCESS_TOKEN] = accessToken
            prefs[Keys.USERNAME] = username
            prefs[Keys.ROLE] = role
        }
    }

    suspend fun updateAccessToken(accessToken: String) {
        dataStore.edit { prefs -> prefs[Keys.ACCESS_TOKEN] = accessToken }
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }
}
