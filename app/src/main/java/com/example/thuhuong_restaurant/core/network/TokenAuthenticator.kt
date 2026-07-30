package com.example.thuhuong_restaurant.core.network

import com.example.thuhuong_restaurant.BuildConfig
import com.example.thuhuong_restaurant.core.data.TokenManager
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

private data class RefreshData(val accessToken: String?)
private data class RefreshEnvelope(val data: RefreshData?)

/**
 * Mirrors AuthContext's web flow: on 401, POST /auth/refresh (refresh_token HttpOnly cookie
 * carried automatically by PersistentCookieJar), swap in the new access token, retry once.
 * Uses a separate, authenticator-free OkHttpClient to avoid recursing into itself.
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenManager: TokenManager,
    @Named("refresh") private val refreshClient: OkHttpClient,
) : Authenticator {

    private val gson = Gson()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= 2) return null

        val newToken = runBlocking {
            runCatching {
                val request = Request.Builder()
                    .url(BuildConfig.API_BASE_URL + "auth/refresh")
                    .post("".toRequestBody(null))
                    .build()
                refreshClient.newCall(request).execute().use { res ->
                    if (!res.isSuccessful) null
                    else res.body?.string()?.let { body ->
                        gson.fromJson(body, RefreshEnvelope::class.java).data?.accessToken
                    }
                }
            }.getOrNull()
        }

        if (newToken.isNullOrBlank()) {
            runBlocking { tokenManager.clear() }
            return null
        }

        runBlocking { tokenManager.updateAccessToken(newToken) }

        return response.request.newBuilder()
            .header("Authorization", "Bearer $newToken")
            .build()
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
