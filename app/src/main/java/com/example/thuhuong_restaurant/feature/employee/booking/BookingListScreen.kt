package com.example.thuhuong_restaurant.feature.employee.booking

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.thuhuong_restaurant.core.common.formatDateTime
import com.example.thuhuong_restaurant.core.common.formatPrice
import com.example.thuhuong_restaurant.core.model.BookingResponse
import com.example.thuhuong_restaurant.core.model.BookingStatus
import com.example.thuhuong_restaurant.core.model.allowsFutureDateFilter
import com.example.thuhuong_restaurant.core.model.label
import com.example.thuhuong_restaurant.core.model.needsRefund
import com.example.thuhuong_restaurant.core.ui.components.CategoryPill
import com.example.thuhuong_restaurant.core.ui.theme.ThSuccess
import com.example.thuhuong_restaurant.core.ui.theme.ThWarning
import com.example.thuhuong_restaurant.feature.employee.DateRangeBar
import com.example.thuhuong_restaurant.feature.employee.EmployeeTabRow

private data class BookingFilterOption(val value: BookingStatus?, val label: String)

private val FILTERS = listOf(
    BookingFilterOption(BookingStatus.CONFIRMED, "Đã xác nhận"),
    BookingFilterOption(BookingStatus.CHECKED_IN, "Đã nhận bàn"),
    BookingFilterOption(BookingStatus.COMPLETED, "Hoàn thành"),
    BookingFilterOption(null, "Tất cả"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingListScreen(
    navController: NavHostController,
    onOpenDetail: (String) -> Unit,
    onOrderReady: (String) -> Unit,
    viewModel: BookingListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.navigateToOrderId) {
        state.navigateToOrderId?.let {
            onOrderReady(it)
            viewModel.onNavigationConsumed()
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Đặt bàn") },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                )
                EmployeeTabRow(navController)
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FILTERS.forEach { option ->
                    CategoryPill(
                        label = option.label,
                        selected = state.statusFilter == option.value,
                        onClick = { viewModel.selectFilter(option.value) },
                    )
                }
            }
            DateRangeBar(
                state = state.dateRange,
                onChange = viewModel::updateDateRange,
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 12.dp),
                allowFuture = state.statusFilter.allowsFutureDateFilter(),
            )

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    state.isLoading -> CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    state.error != null -> Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(state.error!!, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = viewModel::retry) { Text("Thử lại", color = MaterialTheme.colorScheme.primary) }
                    }
                    state.bookings.isEmpty() -> Text(
                        "Không có đặt bàn nào",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center),
                    )
                    else -> LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(state.bookings, key = { it.id }) { booking ->
                            BookingRow(
                                booking = booking,
                                onDetail = { onOpenDetail(booking.bookingCode) },
                                onCheckout = { viewModel.openCheckoutDialog(booking) },
                                onComplete = { viewModel.openCompleteDialog(booking) },
                            )
                        }
                        if (state.hasMore) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    if (state.isLoadingMore) {
                                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                    } else {
                                        TextButton(onClick = viewModel::loadMore) {
                                            Text("Tải thêm", color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    state.checkoutTarget?.let {
        TableSelectionDialog(
            tables = state.tables,
            isLoadingTables = state.isLoadingTables,
            selectedTableId = state.selectedTableId,
            onSelectTable = viewModel::selectTable,
            isSubmitting = state.isCheckingOut,
            error = state.checkoutError,
            onConfirm = viewModel::confirmCheckout,
            onDismiss = viewModel::dismissCheckoutDialog,
        )
    }

    state.completeTarget?.let { booking ->
        CompleteBookingDialog(
            booking = booking,
            isCompleting = state.isCompleting,
            error = state.completeError,
            onConfirm = viewModel::confirmComplete,
            onDismiss = viewModel::dismissCompleteDialog,
        )
    }
}

@Composable
private fun BookingRow(
    booking: BookingResponse,
    onDetail: () -> Unit,
    onCheckout: () -> Unit,
    onComplete: () -> Unit,
) {
    val needsRefund = booking.needsRefund()

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
        border = BorderStroke(1.dp, if (needsRefund) ThWarning else MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(booking.bookingCode, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Text(
                    if (needsRefund) "💰 Cần hoàn" else booking.status.label(),
                    fontWeight = FontWeight.SemiBold,
                    color = if (needsRefund) ThWarning else ThSuccess,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text("${booking.customerName} · ${booking.customerPhone}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatDateTime(booking.scheduledTime), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(formatPrice(booking.totalAmount), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDetail, shape = MaterialTheme.shapes.small, modifier = Modifier.weight(1f)) {
                    Text("Chi tiết")
                }
                if (booking.status == BookingStatus.CONFIRMED) {
                    OutlinedButton(onClick = onCheckout, shape = MaterialTheme.shapes.small, modifier = Modifier.weight(1f)) {
                        Text("→ Mở đơn")
                    }
                }
                if (booking.status == BookingStatus.CHECKED_IN) {
                    OutlinedButton(onClick = onComplete, shape = MaterialTheme.shapes.small, modifier = Modifier.weight(1f)) {
                        Text("✓ Hoàn thành")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompleteBookingDialog(
    booking: BookingResponse,
    isCompleting: Boolean,
    error: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Hoàn thành đặt bàn ${booking.bookingCode}?") },
        text = {
            if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isCompleting) {
                Text("Xác nhận", color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        },
    )
}
