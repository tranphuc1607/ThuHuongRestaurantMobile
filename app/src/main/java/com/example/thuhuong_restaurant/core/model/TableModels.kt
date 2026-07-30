package com.example.thuhuong_restaurant.core.model

enum class TableStatus { AVAILABLE, OCCUPIED }

enum class TableZone { INDOOR_FLOOR_1, INDOOR_FLOOR_2, OUTDOOR }

fun TableZone.label(): String = when (this) {
    TableZone.INDOOR_FLOOR_1 -> "Trong nhà - Tầng 1"
    TableZone.INDOOR_FLOOR_2 -> "Trong nhà - Tầng 2"
    TableZone.OUTDOOR -> "Ngoài trời"
}

data class TableResponse(
    val id: String,
    val tableNumber: Int,
    val name: String,
    val qrToken: String,
    val status: TableStatus,
    val zone: TableZone,
    val note: String?,
)
