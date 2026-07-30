package com.example.thuhuong_restaurant.core.model

data class UserResponse(
    val id: String?,
    val username: String?,
    val email: String?,
    val fullName: String?,
    val phone: String?,
    val role: String?,
    val enabled: Boolean = false,
    val status: String?,
)
