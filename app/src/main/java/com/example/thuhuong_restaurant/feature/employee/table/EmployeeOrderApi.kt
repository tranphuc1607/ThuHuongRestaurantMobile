package com.example.thuhuong_restaurant.feature.employee.table

import com.example.thuhuong_restaurant.core.model.AddOrderItemRequest
import com.example.thuhuong_restaurant.core.model.AddSurchargeRequest
import com.example.thuhuong_restaurant.core.model.CreateOrderRequest
import com.example.thuhuong_restaurant.core.model.OrderResponse
import com.example.thuhuong_restaurant.core.model.UpdateItemQtyRequest
import com.example.thuhuong_restaurant.core.network.PageResponse
import com.example.thuhuong_restaurant.core.network.RestResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface EmployeeOrderApi {
    @GET("orders")
    suspend fun getOrders(
        @Query("page") page: Int,
        @Query("size") size: Int,
        @Query("status") status: String?,
        @Query("from") from: String?,
        @Query("to") to: String?,
    ): RestResponse<PageResponse<OrderResponse>>

    @POST("orders")
    suspend fun createOrder(@Body request: CreateOrderRequest): RestResponse<OrderResponse>

    @GET("orders/table/{qrToken}")
    suspend fun getOrderByTableQrToken(@Path("qrToken") qrToken: String): RestResponse<OrderResponse>

    @GET("orders/{orderId}")
    suspend fun getOrder(@Path("orderId") orderId: String): RestResponse<OrderResponse>

    @POST("orders/{orderId}/cancel")
    suspend fun cancelOrder(
        @Path("orderId") orderId: String,
        @Body body: Map<String, String?>,
    ): RestResponse<OrderResponse>

    @POST("orders/{orderId}/surcharges")
    suspend fun addSurcharge(
        @Path("orderId") orderId: String,
        @Body request: AddSurchargeRequest,
    ): RestResponse<OrderResponse>

    @DELETE("orders/{orderId}/surcharges/{surchargeId}")
    suspend fun removeSurcharge(
        @Path("orderId") orderId: String,
        @Path("surchargeId") surchargeId: String,
    ): RestResponse<OrderResponse>

    @POST("orders/{orderId}/items")
    suspend fun addItem(
        @Path("orderId") orderId: String,
        @Body request: AddOrderItemRequest,
    ): RestResponse<OrderResponse>

    @PATCH("orders/{orderId}/items/{itemId}")
    suspend fun updateItemQuantity(
        @Path("orderId") orderId: String,
        @Path("itemId") itemId: String,
        @Body request: UpdateItemQtyRequest,
    ): RestResponse<OrderResponse>

    @DELETE("orders/{orderId}/items/{itemId}")
    suspend fun removeItem(
        @Path("orderId") orderId: String,
        @Path("itemId") itemId: String,
    ): RestResponse<OrderResponse>

    @POST("orders/{orderId}/checkout")
    suspend fun checkout(@Path("orderId") orderId: String): RestResponse<OrderResponse>
}
