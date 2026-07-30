package com.example.thuhuong_restaurant.core.model

enum class ProductCategory {
    CHICKEN, PORK, BEEF, FISH, VEGETABLE, DRINK_FOOD, DRINK, OTHER
}

enum class ProductStatus {
    AVAILABLE, OUT_OF_STOCK
}

data class ProductResponse(
    val id: String,
    val name: String,
    val price: Double,
    val description: String?,
    val imageUrl: String?,
    val category: ProductCategory,
    val status: ProductStatus,
    val saleUnit: String?,
)

fun ProductCategory.label(): String = when (this) {
    ProductCategory.CHICKEN -> "Món gà"
    ProductCategory.PORK -> "Món heo"
    ProductCategory.BEEF -> "Món bò"
    ProductCategory.FISH -> "Món cá"
    ProductCategory.VEGETABLE -> "Rau củ"
    ProductCategory.DRINK_FOOD -> "Đồ ăn kèm"
    ProductCategory.DRINK -> "Đồ uống"
    ProductCategory.OTHER -> "Khác"
}
