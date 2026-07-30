package com.example.thuhuong_restaurant.feature.employee.receiptscan

import com.example.thuhuong_restaurant.core.common.ApiResult
import com.example.thuhuong_restaurant.core.model.ReceiptScanResponse
import com.example.thuhuong_restaurant.core.model.ScanFeedbackRequest
import com.example.thuhuong_restaurant.core.model.ScanReceiptRequest
import com.example.thuhuong_restaurant.core.network.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReceiptScanRepository @Inject constructor(
    private val api: ReceiptScanApi,
) {
    suspend fun scanReceipt(imagesBase64: List<String>, mimeType: String): ApiResult<ReceiptScanResponse> =
        safeApiCall { api.scanReceipt(ScanReceiptRequest(imagesBase64, mimeType)) }

    /**
     * Gửi cặp (chữ viết tay → món) mà nhân viên đã xác nhận. Chỉ để cải thiện dần độ chính xác nên
     * lỗi ở đây không được ảnh hưởng luồng tạo đơn — caller bỏ qua kết quả.
     */
    suspend fun sendFeedback(items: List<ScanFeedbackRequest.Item>): ApiResult<Map<String, Any>> =
        safeApiCall { api.sendFeedback(ScanFeedbackRequest(items)) }
}
