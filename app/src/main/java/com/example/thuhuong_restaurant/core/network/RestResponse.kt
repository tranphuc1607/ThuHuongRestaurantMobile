package com.example.thuhuong_restaurant.core.network

/**
 * Mirrors restaurant-common's RestResponse<T> — every gateway response is wrapped in this envelope.
 */
data class RestResponse<T>(
    val statusCode: Int,
    val message: String?,
    val data: T?,
    val error: String?,
)

data class PageResponse<T>(
    val content: List<T>,
    val totalElements: Long,
    val totalPages: Int,
    val number: Int,
    val size: Int,
    val empty: Boolean,
)

private val GENERIC_EN = setOf(
    "Bad Request", "Unauthorized", "Forbidden", "Not Found",
    "Internal Server Error", "Service Unavailable",
    "Business Logic Exception", "OK", "Conflict", "Unprocessable Entity",
)

private val TECHNICAL_PATTERNS = listOf(
    Regex("Exception"),
    Regex("""\.(java|kt)"""),
    Regex("""com\.(example|spring|mysql|hibernate)"""),
    Regex("""org\.(springframework|hibernate)"""),
    Regex("ConstraintViolation"),
    Regex("DataIntegrity"),
)

private fun isTechnical(s: String) = TECHNICAL_PATTERNS.any { it.containsMatchIn(s) }

private fun isUsable(s: String?) = !s.isNullOrBlank() && s !in GENERIC_EN && !isTechnical(s)

/** message vs error field usage is inconsistent across services — pick whichever isn't a generic/technical string. */
fun RestResponse<*>.extractApiError(fallback: String): String {
    if (isUsable(message)) return message!!
    if (isUsable(error)) return error!!
    return fallback
}
