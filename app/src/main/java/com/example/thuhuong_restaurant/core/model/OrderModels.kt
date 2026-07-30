package com.example.thuhuong_restaurant.core.model

enum class OrderStatus { OPEN, PAID, CANCELLED }

data class OrderResponse(
    val id: String,
    val tableId: String?,
    val tableName: String?,
    val tableNumber: Int?,
    val tableQrToken: String?,
    val status: OrderStatus,
    val totalAmount: Double,
    val prepaidAmount: Double,
    val createdBy: String?,
    val note: String?,
    val cancelReason: String?,
    val createdAt: String? = null,
    val items: List<OrderItemResponse> = emptyList(),
    val surcharges: List<OrderSurchargeResponse> = emptyList(),
)

data class OrderItemResponse(
    val id: String,
    val productId: String,
    val productName: String,
    val productPrice: Double,
    val quantity: Int,
    val saleUnit: String,
    val itemTotal: Double,
    val category: String?,
    val fromBooking: Boolean = false,
)

data class OrderSurchargeResponse(
    val id: String,
    val name: String,
    val unitPrice: Double,
    val quantity: Int,
    val amount: Double,
    val reason: String?,
)

data class CreateOrderRequest(
    val tableId: String? = null,
    val note: String? = null,
)

data class AddOrderItemRequest(
    val productId: String,
    val quantity: Int,
    val saleUnit: String,
    val skipInventoryDeduction: Boolean = false,
)

data class UpdateItemQtyRequest(
    val quantity: Int,
)

data class AddSurchargeRequest(
    val name: String,
    val unitPrice: Double,
    val quantity: Int,
    val reason: String? = null,
)

/** round-half-up to nearest đồng, mirrors the web's Math.round() usage for refund/needsRefund math. */
fun netRefundOf(prepaidAmount: Double, totalAmount: Double): Double =
    kotlin.math.round(prepaidAmount - totalAmount)

fun OrderResponse.needsRefund(): Boolean =
    status == OrderStatus.OPEN && netRefundOf(prepaidAmount, totalAmount) > 0

/** Orders converted from a booking checkout cannot be cancelled (server enforces this too). */
fun OrderResponse.hasBookingItems(): Boolean = items.any { it.fromBooking }

/** Mirrors the web's per-category default sale unit fallback in order-detail's add-item flow. */
private val DEFAULT_SALE_UNIT_BY_CATEGORY = mapOf(
    ProductCategory.DRINK to "CUP",
    ProductCategory.DRINK_FOOD to "PORTION",
    ProductCategory.CHICKEN to "PORTION",
    ProductCategory.PORK to "PORTION",
    ProductCategory.BEEF to "PORTION",
    ProductCategory.FISH to "PORTION",
    ProductCategory.VEGETABLE to "PORTION",
    ProductCategory.OTHER to "PORTION",
)

fun defaultSaleUnitFor(product: ProductResponse): String =
    product.saleUnit?.takeIf { it.isNotBlank() }
        ?: DEFAULT_SALE_UNIT_BY_CATEGORY[product.category]
        ?: "PORTION"

/** Nhãn tiếng Việt của đơn vị bán — khớp SALE_UNITS bên web. */
fun saleUnitLabel(saleUnit: String): String = when (saleUnit.uppercase()) {
    "PORTION" -> "Phần"
    "CUP" -> "Ly"
    "DISH" -> "Đĩa/Tô"
    "PIECE" -> "Cái"
    "CHAI" -> "Chai"
    "KG" -> "Kg"
    else -> saleUnit
}

/**
 * Bia cốc tách riêng khỏi các món khác: khách gọi thành nhiều "đợt" trong một bữa, mỗi đợt là một
 * dòng item riêng, nên để lẫn vào danh sách món thì danh sách bị bia lấn hết. Cách phân loại giống
 * hệt trang chi tiết đơn bên web.
 */
fun isBeerItem(category: String?, productName: String): Boolean =
    category == "DRINK" && productName.lowercase().contains("bia cốc")

fun isBeerProduct(product: ProductResponse): Boolean =
    isBeerItem(product.category.name, product.name)

/** Các đợt bia cốc, kể cả đợt đi kèm từ đặt bàn. */
val OrderResponse.beerItems: List<OrderItemResponse>
    get() = items.filter { isBeerItem(it.category, it.productName) }

/** Món đặt trước từ booking — chỉ đọc, nhân viên không sửa được. */
val OrderResponse.bookingItems: List<OrderItemResponse>
    get() = items.filter { it.fromBooking && !isBeerItem(it.category, it.productName) }

/** Món gọi tại bàn — phần sửa được. */
val OrderResponse.foodItems: List<OrderItemResponse>
    get() = items.filter { !it.fromBooking && !isBeerItem(it.category, it.productName) }
