package com.example.thuhuong_restaurant.feature.employee.booking

import com.example.thuhuong_restaurant.core.model.BookingResponse
import com.example.thuhuong_restaurant.core.model.CheckoutToOrderRequest
import com.example.thuhuong_restaurant.core.model.OrderResponse
import com.example.thuhuong_restaurant.core.network.PageResponse
import com.example.thuhuong_restaurant.core.network.RestResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface BookingsApi {
    @GET("bookings")
    suspend fun getBookings(
        @Query("page") page: Int,
        @Query("size") size: Int,
        @Query("status") status: String?,
        @Query("excludePending") excludePending: Boolean?,
        @Query("from") from: String?,
        @Query("to") to: String?,
    ): RestResponse<PageResponse<BookingResponse>>

    @GET("bookings/{bookingCode}")
    suspend fun getBookingByCode(@Path("bookingCode") bookingCode: String): RestResponse<BookingResponse>

    @PATCH("bookings/{id}/complete")
    suspend fun completeBooking(@Path("id") id: String): RestResponse<BookingResponse>

    @POST("bookings/{id}/checkout-to-order")
    suspend fun checkoutToOrder(
        @Path("id") id: String,
        @Body request: CheckoutToOrderRequest,
    ): RestResponse<OrderResponse>
}
