package com.example.thuhuong_restaurant.core.common

import java.time.LocalDate

enum class DateGranularity(val label: String) {
    DAY("Ngày"),
    WEEK("Tuần"),
    MONTH("Tháng"),
    ALL("Tất cả"),
    CUSTOM("Tùy chỉnh"),
}

/**
 * [offset] steps the window by whole units of [granularity] relative to today — e.g. granularity=DAY,
 * offset=-1 is yesterday; granularity=WEEK, offset=-1 is the 7-day window ending 7 days ago. Lets the
 * employee page back/forward (◀ ▶) without reopening the custom picker for every adjacent day/week/month.
 */
data class DateRangeState(
    val granularity: DateGranularity = DateGranularity.DAY,
    val offset: Int = 0,
    val customFrom: LocalDate? = null,
    val customTo: LocalDate? = null,
) {
    /** Resolved (from, to) dates for the current window — null/null means no filter ("Tất cả"). */
    val resolved: Pair<LocalDate?, LocalDate?>
        get() {
            val today = LocalDate.now(VN_ZONE)
            return when (granularity) {
                DateGranularity.DAY -> {
                    val d = today.plusDays(offset.toLong())
                    d to d
                }
                DateGranularity.WEEK -> {
                    val end = today.plusDays(offset.toLong() * 7)
                    end.minusDays(6) to end
                }
                DateGranularity.MONTH -> {
                    val end = today.plusDays(offset.toLong() * 30)
                    end.minusDays(29) to end
                }
                DateGranularity.ALL -> null to null
                DateGranularity.CUSTOM -> customFrom to customTo
            }
        }

    val fromIso: String? get() = resolved.first?.startOfDayIsoVn()
    val toIso: String? get() = resolved.second?.endOfDayIsoVn()

    /** DAY/WEEK/MONTH windows can be paged with ◀ ▶; ALL and CUSTOM are fixed. */
    val canStep: Boolean
        get() = granularity == DateGranularity.DAY || granularity == DateGranularity.WEEK || granularity == DateGranularity.MONTH

    /** True only for the default "today" window — lets callers prefer a server endpoint with VN-day boundaries baked in. */
    val isToday: Boolean
        get() = granularity == DateGranularity.DAY && offset == 0

    fun steppedBy(delta: Int): DateRangeState = copy(offset = offset + delta)

    val label: String
        get() = when (granularity) {
            DateGranularity.DAY -> when (offset) {
                0 -> "Hôm nay"
                -1 -> "Hôm qua"
                -2 -> "Hôm kia"
                1 -> "Ngày mai"
                2 -> "Ngày kia"
                else -> formatShortDate(resolved.first!!)
            }
            DateGranularity.WEEK -> if (offset == 0) "Tuần này" else formatShortRange()
            DateGranularity.MONTH -> if (offset == 0) "30 ngày gần đây" else formatShortRange()
            DateGranularity.ALL -> "Tất cả"
            DateGranularity.CUSTOM -> {
                val from = customFrom ?: LocalDate.now(VN_ZONE)
                val to = customTo ?: from
                "${formatShortDate(from)} - ${formatShortDate(to)}"
            }
        }

    private fun formatShortRange(): String {
        val (from, to) = resolved
        return "${formatShortDate(from!!)} - ${formatShortDate(to!!)}"
    }
}
