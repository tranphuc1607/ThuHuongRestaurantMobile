package com.example.thuhuong_restaurant.core.model

enum class BookingStatus { PENDING_PAYMENT, CONFIRMED, CHECKED_IN, COMPLETED, CANCELLED, EXPIRED }

enum class BookingPaymentMethod { BANK_TRANSFER, CASH }

fun BookingStatus.label(): String = when (this) {
    BookingStatus.PENDING_PAYMENT -> "Chờ thanh toán"
    BookingStatus.CONFIRMED -> "Đã xác nhận"
    BookingStatus.CHECKED_IN -> "Đã nhận bàn"
    BookingStatus.COMPLETED -> "Hoàn thành"
    BookingStatus.CANCELLED -> "Đã hủy"
    BookingStatus.EXPIRED -> "Hết hạn"
}

fun BookingPaymentMethod.label(): String = when (this) {
    BookingPaymentMethod.BANK_TRANSFER -> "Chuyển khoản"
    BookingPaymentMethod.CASH -> "Tiền mặt"
}

data class BookingItemResponse(
    val id: String,
    val productId: String,
    val productName: String,
    val productPrice: Double,
    val quantity: Int,
    val saleUnit: String,
    val itemTotal: Double,
)

data class BookingResponse(
    val id: String,
    val bookingCode: String,
    val customerName: String,
    val customerPhone: String,
    val customerBankAccount: String?,
    val tableId: String?,
    val tableName: String?,
    val scheduledTime: String,
    val guestCount: Int?,
    val status: BookingStatus,
    val totalAmount: Double,
    val paidAmount: Double,
    val stockWarning: String?,
    val cancelReason: String?,
    val note: String?,
    val paymentMethod: BookingPaymentMethod?,
    val items: List<BookingItemResponse> = emptyList(),
    val createdAt: String? = null,
)

data class CheckoutToOrderRequest(
    val tableId: String? = null,
)

/** Booking's needsRefund is gated on BANK_TRANSFER specifically — different condition than order's needsRefund. */
fun BookingResponse.needsRefund(): Boolean =
    status == BookingStatus.CONFIRMED &&
        paymentMethod == BookingPaymentMethod.BANK_TRANSFER &&
        netRefundOf(paidAmount, totalAmount) > 0

/**
 * Whether the date-range filter for this status tab may page into the future. CHECKED_IN/COMPLETED
 * bookings have already happened (check-in/completion is a real event that occurred today or earlier),
 * so there's nothing to show tomorrow. CONFIRMED (upcoming reservations) and "Tất cả" (a mix that
 * includes CONFIRMED) still need forward paging.
 */
fun BookingStatus?.allowsFutureDateFilter(): Boolean =
    this != BookingStatus.CHECKED_IN && this != BookingStatus.COMPLETED
