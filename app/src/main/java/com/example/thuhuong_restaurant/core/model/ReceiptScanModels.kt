package com.example.thuhuong_restaurant.core.model

data class ScanReceiptRequest(
    val imagesBase64: List<String>,
    val mimeType: String = "image/jpeg",
)

/** Một ứng viên khớp cho dòng hóa đơn — dùng làm chip gợi ý khi độ tin cậy thấp. */
data class MatchCandidate(
    val productId: String,
    val productName: String,
    val price: Double?,
    val score: Int?,
)

data class ScannedItemResponse(
    /** Cột STT trên mẫu hóa đơn. */
    val rowNumber: Int?,
    val productId: String?,
    val productName: String?,
    val quantity: Int,
    /** Server đã lấy từ DB của sản phẩm khớp — không phải giá trị AI đoán. */
    val saleUnit: String,
    val rawText: String?,
    /** Chữ gốc ở cột ĐVT, chỉ để tham chiếu. */
    val rawUnit: String?,
    val unitPrice: Double?,
    val lineTotal: Double?,
    val catalogPrice: Double?,
    val confidence: String,
    val candidates: List<MatchCandidate> = emptyList(),
    val warnings: List<String> = emptyList(),
)

/** Kết quả quét một hóa đơn (có thể nhiều trang). */
data class ReceiptScanResponse(
    val items: List<ScannedItemResponse> = emptyList(),
    /**
     * Hàng "Bia" trên mẫu hóa đơn: mỗi ô là MỘT đợt khách gọi, giữ nguyên thứ tự.
     * `[4, 6, 2]` = ba đợt 4, 6 và 2 cốc — không phải một đợt 12 cốc.
     */
    val beerRounds: List<Int> = emptyList(),
    val beerProductId: String?,
    val beerProductName: String?,
    val beerUnitPrice: Double?,
    /** Ô "Bàn số" đọc được — dùng để chọn sẵn bàn. */
    val tableLabel: String?,
    val grandTotal: Double?,
    val computedTotal: Double?,
    val warnings: List<String> = emptyList(),
    val model: String?,
)

/** Gửi lên server để học: chữ viết tay này ứng với món nào (theo xác nhận của nhân viên). */
data class ScanFeedbackRequest(
    val items: List<Item>,
) {
    data class Item(
        val rawText: String,
        val productId: String,
    )
}

enum class ScanConfidence { HIGH, LOW, NONE }

fun ScannedItemResponse.confidenceLevel(): ScanConfidence = when (confidence.lowercase()) {
    "high" -> ScanConfidence.HIGH
    "low" -> ScanConfidence.LOW
    else -> ScanConfidence.NONE
}
