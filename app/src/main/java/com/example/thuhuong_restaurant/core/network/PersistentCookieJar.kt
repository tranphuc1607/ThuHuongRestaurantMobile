package com.example.thuhuong_restaurant.core.network

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists cookies (namely the HttpOnly `refresh_token` set by auth-service on login/refresh)
 * across app restarts. The web front-end gets this for free from the browser; a native client
 * needs to carry it itself so POST /auth/refresh keeps working after the process is killed.
 */
@Singleton
class PersistentCookieJar @Inject constructor(@ApplicationContext context: Context) : CookieJar {

    private val prefs = context.getSharedPreferences("thu_huong_cookies", Context.MODE_PRIVATE)
    private val memoryCache = mutableMapOf<String, MutableList<Cookie>>()

    init {
        prefs.all.forEach { (host, value) ->
            val serialized = value as? String ?: return@forEach
            val url = HttpUrl.Builder().scheme("http").host(host).build()
            val cookies = serialized.split(SEPARATOR)
                .mapNotNull { raw -> runCatching { Cookie.parse(url, raw) }.getOrNull() }
            if (cookies.isNotEmpty()) memoryCache[host] = cookies.toMutableList()
        }
    }

    @Synchronized
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        if (cookies.isEmpty()) return
        val host = url.host
        val existing = memoryCache.getOrPut(host) { mutableListOf() }
        val now = System.currentTimeMillis()
        cookies.forEach { incoming ->
            existing.removeAll { it.name == incoming.name }
            if (incoming.expiresAt > now) existing.add(incoming)
        }
        persist(host, existing)
    }

    @Synchronized
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val host = url.host
        val cookies = memoryCache[host] ?: return emptyList()
        val now = System.currentTimeMillis()
        val valid = cookies.filter { it.expiresAt > now }
        if (valid.size != cookies.size) {
            memoryCache[host] = valid.toMutableList()
            persist(host, valid)
        }
        return valid
    }

    @Synchronized
    fun clear() {
        memoryCache.clear()
        prefs.edit().clear().apply()
    }

    private fun persist(host: String, cookies: List<Cookie>) {
        if (cookies.isEmpty()) {
            prefs.edit().remove(host).apply()
        } else {
            prefs.edit().putString(host, cookies.joinToString(SEPARATOR) { it.toString() }).apply()
        }
    }

    private companion object {
        const val SEPARATOR = "\n"
    }
}
