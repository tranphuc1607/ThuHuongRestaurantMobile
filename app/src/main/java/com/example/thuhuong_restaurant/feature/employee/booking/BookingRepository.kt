package com.example.thuhuong_restaurant.feature.employee.booking

import com.example.thuhuong_restaurant.core.common.ApiResult
import com.example.thuhuong_restaurant.core.model.BookingResponse
import com.example.thuhuong_restaurant.core.model.CheckoutToOrderRequest
import com.example.thuhuong_restaurant.core.model.OrderResponse
import com.example.thuhuong_restaurant.core.network.PageResponse
import com.example.thuhuong_restaurant.core.network.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookingRepository @Inject constructor(
    private val api: BookingsApi,
) {
    suspend fun getBookings(
        status: String?,
        excludePending: Boolean,
        page: Int,
        size: Int = 20,
        from: String? = null,
        to: String? = null,
    ): ApiResult<PageResponse<BookingResponse>> =
        safeApiCall { api.getBookings(page, size, status, excludePending.takeIf { it }, from, to) }

    suspend fun getBookingByCode(bookingCode: String): ApiResult<BookingResponse> =
        safeApiCall { api.getBookingByCode(bookingCode) }

    suspend fun completeBooking(id: String): ApiResult<BookingResponse> =
        safeApiCall { api.completeBooking(id) }

    suspend fun checkoutToOrder(id: String, tableId: String?): ApiResult<OrderResponse> =
        safeApiCall { api.checkoutToOrder(id, CheckoutToOrderRequest(tableId)) }
}
