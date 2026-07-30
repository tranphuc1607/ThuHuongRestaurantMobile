package com.example.thuhuong_restaurant.feature.employee.payment

import com.example.thuhuong_restaurant.core.common.ApiResult
import com.example.thuhuong_restaurant.core.model.ChangePaymentMethodRequest
import com.example.thuhuong_restaurant.core.model.CreatePaymentRequest
import com.example.thuhuong_restaurant.core.model.PaymentMethod
import com.example.thuhuong_restaurant.core.model.PaymentResponse
import com.example.thuhuong_restaurant.core.model.RefundRequest
import com.example.thuhuong_restaurant.core.model.UpdateAmountRequest
import com.example.thuhuong_restaurant.core.network.PageResponse
import com.example.thuhuong_restaurant.core.network.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaymentRepository @Inject constructor(
    private val api: PaymentsApi,
) {
    suspend fun createPayment(orderId: String, method: PaymentMethod, amount: Double? = null): ApiResult<PaymentResponse> =
        safeApiCall { api.createPayment(CreatePaymentRequest(orderId = orderId, paymentMethod = method, amount = amount)) }

    suspend fun updateAmount(id: String, newAmount: Double): ApiResult<PaymentResponse> =
        safeApiCall { api.updateAmount(id, UpdateAmountRequest(newAmount = newAmount)) }

    suspend fun changeMethod(id: String, newMethod: PaymentMethod): ApiResult<PaymentResponse> =
        safeApiCall { api.changeMethod(id, ChangePaymentMethodRequest(newMethod = newMethod)) }

    suspend fun confirmPayment(id: String): ApiResult<PaymentResponse> = safeApiCall { api.confirmPayment(id) }

    suspend fun getPayment(id: String): ApiResult<PaymentResponse> = safeApiCall { api.getPayment(id) }

    suspend fun getPaymentByOrder(orderId: String): ApiResult<PaymentResponse> =
        safeApiCall { api.getPaymentByOrder(orderId) }

    suspend fun getAllByOrder(orderId: String): ApiResult<List<PaymentResponse>> =
        safeApiCall { api.getAllByOrder(orderId) }

    suspend fun refund(orderId: String, amount: Double? = null, note: String? = null): ApiResult<PaymentResponse> =
        safeApiCall { api.refund(orderId, RefundRequest(amount = amount, note = note)) }

    suspend fun getToday(status: String?, page: Int, size: Int = 50): ApiResult<PageResponse<PaymentResponse>> =
        safeApiCall { api.getToday(page, size, status) }

    suspend fun getAll(status: String?, from: String?, to: String?, page: Int, size: Int = 50): ApiResult<PageResponse<PaymentResponse>> =
        safeApiCall { api.getAll(page, size, status, from, to) }
}
