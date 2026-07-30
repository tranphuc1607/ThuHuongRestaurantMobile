package com.example.thuhuong_restaurant.feature.employee.order

import com.example.thuhuong_restaurant.core.common.ApiResult
import com.example.thuhuong_restaurant.core.model.AddOrderItemRequest
import com.example.thuhuong_restaurant.core.model.AddSurchargeRequest
import com.example.thuhuong_restaurant.core.model.CreateOrderRequest
import com.example.thuhuong_restaurant.core.model.OrderResponse
import com.example.thuhuong_restaurant.core.model.UpdateItemQtyRequest
import com.example.thuhuong_restaurant.core.network.PageResponse
import com.example.thuhuong_restaurant.core.network.safeApiCall
import com.example.thuhuong_restaurant.feature.employee.table.EmployeeOrderApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrderRepository @Inject constructor(
    private val api: EmployeeOrderApi,
) {
    /** [tableId] optional — a null table means a table-less "quầy" order (used by receipt-scan). */
    suspend fun createOrder(tableId: String?, note: String?): ApiResult<OrderResponse> =
        safeApiCall { api.createOrder(CreateOrderRequest(tableId = tableId, note = note)) }

    suspend fun getOrders(
        status: String?,
        page: Int,
        size: Int = 20,
        from: String? = null,
        to: String? = null,
    ): ApiResult<PageResponse<OrderResponse>> =
        safeApiCall { api.getOrders(page, size, status, from, to) }

    suspend fun getOrder(orderId: String): ApiResult<OrderResponse> = safeApiCall { api.getOrder(orderId) }

    suspend fun addItem(
        orderId: String,
        productId: String,
        quantity: Int,
        saleUnit: String,
        skipInventoryDeduction: Boolean = false,
    ): ApiResult<OrderResponse> =
        safeApiCall { api.addItem(orderId, AddOrderItemRequest(productId, quantity, saleUnit, skipInventoryDeduction)) }

    suspend fun updateItemQuantity(orderId: String, itemId: String, quantity: Int): ApiResult<OrderResponse> =
        safeApiCall { api.updateItemQuantity(orderId, itemId, UpdateItemQtyRequest(quantity)) }

    suspend fun removeItem(orderId: String, itemId: String): ApiResult<OrderResponse> =
        safeApiCall { api.removeItem(orderId, itemId) }

    suspend fun checkout(orderId: String): ApiResult<OrderResponse> = safeApiCall { api.checkout(orderId) }

    suspend fun cancelOrder(orderId: String, reason: String?): ApiResult<OrderResponse> =
        safeApiCall { api.cancelOrder(orderId, mapOf("reason" to reason)) }

    suspend fun addSurcharge(orderId: String, name: String, unitPrice: Double, quantity: Int, reason: String?): ApiResult<OrderResponse> =
        safeApiCall { api.addSurcharge(orderId, AddSurchargeRequest(name, unitPrice, quantity, reason)) }

    suspend fun removeSurcharge(orderId: String, surchargeId: String): ApiResult<OrderResponse> =
        safeApiCall { api.removeSurcharge(orderId, surchargeId) }
}
