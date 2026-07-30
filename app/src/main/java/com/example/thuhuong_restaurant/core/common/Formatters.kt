package com.example.thuhuong_restaurant.core.common

import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val vnFormat = NumberFormat.getNumberInstance(Locale("vi", "VN"))

fun formatPrice(price: Double?): String {
    if (price == null) return "0đ"
    return vnFormat.format(price) + "đ"
}

val VN_ZONE: ZoneId = ZoneId.of("Asia/Ho_Chi_Minh")
private val DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy")
private val SHORT_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy")

/** Parses an ISO-8601 UTC instant and formats it in Vietnam time (UTC+7), e.g. "18:00 - 29/07/2026". */
fun formatDateTime(iso: String?): String {
    if (iso.isNullOrBlank()) return "--"
    return try {
        Instant.parse(iso).atZone(VN_ZONE).format(DATE_TIME_FORMATTER)
    } catch (e: Exception) {
        iso
    }
}

fun formatShortDate(date: LocalDate): String = date.format(SHORT_DATE_FORMATTER)

/** ISO-8601 UTC instant for 00:00:00 of this date in Vietnam time — for a "from" query param. */
fun LocalDate.startOfDayIsoVn(): String = atStartOfDay(VN_ZONE).toInstant().toString()

/** ISO-8601 UTC instant for 23:59:59 of this date in Vietnam time — for a "to" query param. */
fun LocalDate.endOfDayIsoVn(): String = atTime(23, 59, 59).atZone(VN_ZONE).toInstant().toString()
