package com.example.thuhuong_restaurant.feature.auth

import com.example.thuhuong_restaurant.core.model.LoginRequest
import com.example.thuhuong_restaurant.core.model.LoginResponse
import com.example.thuhuong_restaurant.core.model.RegisterRequest
import com.example.thuhuong_restaurant.core.network.RestResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): RestResponse<Any?>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): RestResponse<LoginResponse>

    @POST("auth/logout")
    suspend fun logout(): RestResponse<Any?>
}
