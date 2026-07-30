package com.example.thuhuong_restaurant.feature.menu

import com.example.thuhuong_restaurant.core.common.ApiResult
import com.example.thuhuong_restaurant.core.model.ProductResponse
import com.example.thuhuong_restaurant.core.network.PageResponse
import com.example.thuhuong_restaurant.core.network.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MenuRepository @Inject constructor(
    private val productsApi: ProductsApi,
) {
    suspend fun getProducts(
        category: String?,
        keyword: String?,
        page: Int,
        size: Int = 20,
    ): ApiResult<PageResponse<ProductResponse>> = safeApiCall {
        productsApi.getProducts(
            category = category,
            keyword = keyword?.takeIf { it.isNotBlank() },
            page = page,
            size = size,
        )
    }

    suspend fun getProduct(id: String): ApiResult<ProductResponse> = safeApiCall {
        productsApi.getProduct(id)
    }
}
