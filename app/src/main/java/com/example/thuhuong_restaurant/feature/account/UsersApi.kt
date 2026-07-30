package com.example.thuhuong_restaurant.feature.account

import com.example.thuhuong_restaurant.core.model.UserResponse
import com.example.thuhuong_restaurant.core.network.RestResponse
import retrofit2.http.GET

interface UsersApi {
    @GET("users/me")
    suspend fun me(): RestResponse<UserResponse>
}
