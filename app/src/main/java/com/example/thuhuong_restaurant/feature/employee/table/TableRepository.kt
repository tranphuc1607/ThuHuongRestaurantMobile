package com.example.thuhuong_restaurant.feature.employee.table

import com.example.thuhuong_restaurant.core.common.ApiResult
import com.example.thuhuong_restaurant.core.model.CreateOrderRequest
import com.example.thuhuong_restaurant.core.model.OrderResponse
import com.example.thuhuong_restaurant.core.model.TableResponse
import com.example.thuhuong_restaurant.core.network.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TableRepository @Inject constructor(
    private val tablesApi: TablesApi,
    private val orderApi: EmployeeOrderApi,
) {
    suspend fun getTables(): ApiResult<List<TableResponse>> = safeApiCall { tablesApi.getTables() }

    suspend fun createOrder(tableId: String, note: String?): ApiResult<OrderResponse> =
        safeApiCall { orderApi.createOrder(CreateOrderRequest(tableId = tableId, note = note)) }

    suspend fun getOrderByTableQrToken(qrToken: String): ApiResult<OrderResponse> =
        safeApiCall { orderApi.getOrderByTableQrToken(qrToken) }
}
