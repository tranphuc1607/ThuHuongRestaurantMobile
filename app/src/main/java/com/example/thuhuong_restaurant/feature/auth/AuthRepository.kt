package com.example.thuhuong_restaurant.feature.auth

import com.example.thuhuong_restaurant.core.common.ApiResult
import com.example.thuhuong_restaurant.core.data.TokenManager
import com.example.thuhuong_restaurant.core.model.LoginRequest
import com.example.thuhuong_restaurant.core.model.RegisterRequest
import com.example.thuhuong_restaurant.core.model.UserResponse
import com.example.thuhuong_restaurant.core.network.PersistentCookieJar
import com.example.thuhuong_restaurant.core.network.safeApiCall
import com.example.thuhuong_restaurant.core.network.safeApiCallUnit
import com.example.thuhuong_restaurant.feature.account.UsersApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val authApi: AuthApi,
    private val usersApi: UsersApi,
    private val tokenManager: TokenManager,
    private val cookieJar: PersistentCookieJar,
) {
    val session = tokenManager.session

    suspend fun login(username: String, password: String): ApiResult<UserResponse> {
        val loginResult = safeApiCall { authApi.login(LoginRequest(username, password)) }
        if (loginResult is ApiResult.Failure) return loginResult
        val login = (loginResult as ApiResult.Success).data
        tokenManager.saveSession(
            accessToken = login.accessToken,
            username = login.user?.username ?: username,
            role = login.user?.role ?: "ROLE_USER",
        )
        return fetchMe()
    }

    suspend fun register(username: String, password: String): ApiResult<Unit> =
        safeApiCallUnit { authApi.register(RegisterRequest(username, password)) }

    /** Rehydrates profile from the access token — used both right after login and on app startup. */
    suspend fun fetchMe(): ApiResult<UserResponse> = safeApiCall { usersApi.me() }

    suspend fun logout() {
        safeApiCallUnit { authApi.logout() }
        tokenManager.clear()
        cookieJar.clear()
    }
}
