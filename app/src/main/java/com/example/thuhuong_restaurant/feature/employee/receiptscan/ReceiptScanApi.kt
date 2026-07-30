package com.example.thuhuong_restaurant.feature.employee.receiptscan

import com.example.thuhuong_restaurant.core.model.ReceiptScanResponse
import com.example.thuhuong_restaurant.core.model.ScanFeedbackRequest
import com.example.thuhuong_restaurant.core.model.ScanReceiptRequest
import com.example.thuhuong_restaurant.core.network.RestResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface ReceiptScanApi {
    @POST("orders/scan-receipt")
    suspend fun scanReceipt(@Body request: ScanReceiptRequest): RestResponse<ReceiptScanResponse>

    /** Ghi nhận cách viết tay → món, để lần quét sau chính xác hơn. */
    @POST("orders/scan-receipt/feedback")
    suspend fun sendFeedback(@Body request: ScanFeedbackRequest): RestResponse<Map<String, Any>>
}
