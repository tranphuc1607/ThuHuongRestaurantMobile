package com.example.thuhuong_restaurant.feature.employee

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thuhuong_restaurant.core.common.DateGranularity
import com.example.thuhuong_restaurant.core.common.DateRangeState
import com.example.thuhuong_restaurant.core.common.formatShortDate
import com.example.thuhuong_restaurant.core.ui.components.CategoryPill
import java.time.LocalDate

private val GRANULARITIES = listOf(DateGranularity.DAY, DateGranularity.WEEK, DateGranularity.MONTH, DateGranularity.ALL)

/**
 * Granularity pills (Ngày / Tuần / Tháng / Tất cả / Tùy chỉnh) + a ◀ label ▶ stepper for the
 * steppable granularities, so an employee can page to yesterday/last week/etc. without opening
 * the custom-range picker every time.
 *
 * @param allowFuture whether "›" may step past today's window. Order/payment history can never have
 * future rows, so those screens pass `false` (default) to grey out "›" once the window reaches today;
 * bookings are scheduled ahead of time, so that screen passes `true`.
 */
@Composable
fun DateRangeBar(
    state: DateRangeState,
    onChange: (DateRangeState) -> Unit,
    modifier: Modifier = Modifier,
    allowFuture: Boolean = false,
) {
    val context = LocalContext.current

    fun openPicker(initial: LocalDate?, onPicked: (LocalDate) -> Unit) {
        val base = initial ?: LocalDate.now()
        DatePickerDialog(
            context,
            { _, year, month, day -> onPicked(LocalDate.of(year, month + 1, day)) },
            base.year, base.monthValue - 1, base.dayOfMonth,
        ).show()
    }

    fun pickCustomRange() {
        openPicker(state.customFrom) { from ->
            openPicker(state.customTo ?: from) { to ->
                val (lo, hi) = if (to.isBefore(from)) to to from else from to to
                onChange(state.copy(granularity = DateGranularity.CUSTOM, customFrom = lo, customTo = hi))
            }
        }
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GRANULARITIES.forEach { granularity ->
                CategoryPill(
                    label = granularity.label,
                    selected = state.granularity == granularity,
                    onClick = { onChange(state.copy(granularity = granularity, offset = 0)) },
                )
            }
            CategoryPill(
                label = if (state.granularity == DateGranularity.CUSTOM) state.label else "Tùy chỉnh",
                selected = state.granularity == DateGranularity.CUSTOM,
                onClick = ::pickCustomRange,
            )
        }

        if (state.canStep) {
            val nextDisabled = !allowFuture && state.offset >= 0
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StepArrow("‹", enabled = true) { onChange(state.steppedBy(-1)) }
                Text(
                    state.label,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                StepArrow("›", enabled = !nextDisabled) { onChange(state.steppedBy(1)) }
            }
        }
    }
}

@Composable
private fun StepArrow(symbol: String, enabled: Boolean, onClick: () -> Unit) {
    Text(
        symbol,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        modifier = Modifier
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 4.dp),
    )
}
