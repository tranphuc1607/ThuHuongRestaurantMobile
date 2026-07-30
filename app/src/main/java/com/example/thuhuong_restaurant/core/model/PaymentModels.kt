package com.example.thuhuong_restaurant.core.model

enum class PaymentMethod { CASH, BANK_TRANSFER, CARD, REFUND }

enum class PaymentStatus { PENDING, COMPLETED, FAILED, REFUNDED }

fun PaymentMethod.label(): String = when (this) {
    PaymentMethod.CASH -> "Tiền mặt"
    PaymentMethod.BANK_TRANSFER -> "Chuyển khoản"
    PaymentMethod.CARD -> "Thẻ"
    PaymentMethod.REFUND -> "Hoàn tiền"
}

data class PaymentResponse(
    val id: String,
    val orderId: String,
    val amount: Double,
    val paymentMethod: PaymentMethod,
    val status: PaymentStatus,
    val vietQrUrl: String?,
    val referenceCode: String?,
    val paidBy: String?,
    val note: String?,
    val tableName: String? = null,
    val tableNumber: Int? = null,
    val createdAt: String? = null,
)

data class CreatePaymentRequest(
    val orderId: String,
    val paymentMethod: PaymentMethod,
    val amount: Double? = null,
    val note: String? = null,
)

data class ChangePaymentMethodRequest(
    val newMethod: PaymentMethod,
)

data class UpdateAmountRequest(
    val newAmount: Double,
)

data class RefundRequest(
    val amount: Double? = null,
    val note: String? = null,
)
