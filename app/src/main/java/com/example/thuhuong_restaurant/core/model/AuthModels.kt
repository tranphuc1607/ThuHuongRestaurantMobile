package com.example.thuhuong_restaurant.core.model

data class LoginRequest(
    val username: String,
    val password: String,
)

data class RegisterRequest(
    val username: String,
    val password: String,
)

data class LoginResponse(
    val accessToken: String,
    val tokenType: String?,
    val expiresIn: Long?,
    val user: LoginUserInfo?,
)

data class LoginUserInfo(
    val id: String?,
    val username: String?,
    val role: String?,
)
