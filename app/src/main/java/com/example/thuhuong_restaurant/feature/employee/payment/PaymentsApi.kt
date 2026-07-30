package com.example.thuhuong_restaurant.feature.employee.payment

import com.example.thuhuong_restaurant.core.model.ChangePaymentMethodRequest
import com.example.thuhuong_restaurant.core.model.CreatePaymentRequest
import com.example.thuhuong_restaurant.core.model.PaymentResponse
import com.example.thuhuong_restaurant.core.model.RefundRequest
import com.example.thuhuong_restaurant.core.model.UpdateAmountRequest
import com.example.thuhuong_restaurant.core.network.PageResponse
import com.example.thuhuong_restaurant.core.network.RestResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface PaymentsApi {
    @POST("payments")
    suspend fun createPayment(@Body request: CreatePaymentRequest): RestResponse<PaymentResponse>

    @PATCH("payments/{id}/amount")
    suspend fun updateAmount(@Path("id") id: String, @Body request: UpdateAmountRequest): RestResponse<PaymentResponse>

    @PATCH("payments/{id}/change-method")
    suspend fun changeMethod(@Path("id") id: String, @Body request: ChangePaymentMethodRequest): RestResponse<PaymentResponse>

    @PATCH("payments/{id}/confirm")
    suspend fun confirmPayment(@Path("id") id: String): RestResponse<PaymentResponse>

    @GET("payments/{id}")
    suspend fun getPayment(@Path("id") id: String): RestResponse<PaymentResponse>

    @GET("payments/orders/{orderId}")
    suspend fun getPaymentByOrder(@Path("orderId") orderId: String): RestResponse<PaymentResponse>

    @GET("payments/orders/{orderId}/all")
    suspend fun getAllByOrder(@Path("orderId") orderId: String): RestResponse<List<PaymentResponse>>

    @POST("payments/orders/{orderId}/refund")
    suspend fun refund(@Path("orderId") orderId: String, @Body request: RefundRequest): RestResponse<PaymentResponse>

    @GET("payments/today")
    suspend fun getToday(
        @Query("page") page: Int,
        @Query("size") size: Int,
        @Query("status") status: String?,
    ): RestResponse<PageResponse<PaymentResponse>>

    @GET("payments")
    suspend fun getAll(
        @Query("page") page: Int,
        @Query("size") size: Int,
        @Query("status") status: String?,
        @Query("from") from: String?,
        @Query("to") to: String?,
    ): RestResponse<PageResponse<PaymentResponse>>
}
