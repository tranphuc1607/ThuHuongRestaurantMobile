package com.example.thuhuong_restaurant.feature.menu

import com.example.thuhuong_restaurant.core.model.ProductResponse
import com.example.thuhuong_restaurant.core.network.PageResponse
import com.example.thuhuong_restaurant.core.network.RestResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ProductsApi {
    @GET("products")
    suspend fun getProducts(
        @Query("category") category: String? = null,
        @Query("keyword") keyword: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
    ): RestResponse<PageResponse<ProductResponse>>

    @GET("products/{id}")
    suspend fun getProduct(@Path("id") id: String): RestResponse<ProductResponse>
}
