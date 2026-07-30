package com.example.thuhuong_restaurant.feature.employee.booking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.example.thuhuong_restaurant.core.common.formatDateTime
import com.example.thuhuong_restaurant.core.common.formatPrice
import com.example.thuhuong_restaurant.core.model.BookingItemResponse
import com.example.thuhuong_restaurant.core.model.BookingStatus
import com.example.thuhuong_restaurant.core.model.label
import com.example.thuhuong_restaurant.core.ui.theme.ThWarning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingDetailScreen(
    onBack: () -> Unit,
    onOrderReady: (String) -> Unit,
    viewModel: BookingDetailViewModel = hiltViewModel(),
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
            TopAppBar(
                title = { Text(state.booking?.bookingCode ?: "Đặt bàn") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary,
                )
                state.error != null -> Text(
                    state.error!!,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center),
                )
                state.booking != null -> {
                    val booking = state.booking!!
                    Column(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .padding(20.dp),
                        ) {
                            if (!booking.stockWarning.isNullOrBlank()) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = ThWarning.copy(alpha = 0.15f)),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(
                                        "⚠️ ${booking.stockWarning}",
                                        color = ThWarning,
                                        modifier = Modifier.padding(12.dp),
                                    )
                                }
                                Spacer(Modifier.height(12.dp))
                            }

                            Text("Khách hàng", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                            Text(booking.customerName, color = MaterialTheme.colorScheme.onBackground)
                            Text(booking.customerPhone, color = MaterialTheme.colorScheme.onSurfaceVariant)

                            Spacer(Modifier.height(16.dp))
                            Text("Thông tin đặt bàn", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                            InfoRow("Trạng thái", booking.status.label())
                            InfoRow("Giờ hẹn", formatDateTime(booking.scheduledTime))
                            InfoRow("Bàn", booking.tableName ?: "Chưa xếp bàn")
                            InfoRow("Phương thức", booking.paymentMethod?.label() ?: "--")
                            InfoRow("Tổng cọc", formatPrice(booking.paidAmount))
                            InfoRow("Tổng tiền", formatPrice(booking.totalAmount))

                            if (booking.items.isNotEmpty()) {
                                Spacer(Modifier.height(16.dp))
                                Text("Món đã đặt trước", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                                    items(booking.items, key = { it.id }) { item -> BookingItemRow(item) }
                                }
                            }

                            if (!booking.note.isNullOrBlank()) {
                                Spacer(Modifier.height(16.dp))
                                Text("Ghi chú", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                                Text(booking.note, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            if (booking.status == BookingStatus.CANCELLED && !booking.cancelReason.isNullOrBlank()) {
                                Spacer(Modifier.height(16.dp))
                                Text("Lý do hủy", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                Text(booking.cancelReason, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        val hasActions = booking.status == BookingStatus.CONFIRMED || booking.status == BookingStatus.CHECKED_IN
                        if (hasActions) {
                            Row(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                                if (booking.status == BookingStatus.CONFIRMED) {
                                    Button(
                                        onClick = viewModel::openCheckoutDialog,
                                        shape = MaterialTheme.shapes.small,
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        modifier = Modifier.fillMaxWidth(),
                                    ) { Text("🧾 Chuyển sang đơn") }
                                }
                                if (booking.status == BookingStatus.CHECKED_IN) {
                                    Button(
                                        onClick = viewModel::openCompleteDialog,
                                        shape = MaterialTheme.shapes.small,
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        modifier = Modifier.fillMaxWidth(),
                                    ) { Text("✅ Hoàn thành") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (state.showCheckoutDialog) {
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

    if (state.showCompleteDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = viewModel::dismissCompleteDialog,
            title = { Text("Hoàn thành đặt bàn?") },
            text = { if (state.completeError != null) Text(state.completeError!!, color = MaterialTheme.colorScheme.error) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = viewModel::confirmComplete, enabled = !state.isCompleting) {
                    Text("Xác nhận", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = viewModel::dismissCompleteDialog) { Text("Hủy") }
            },
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun BookingItemRow(item: BookingItemResponse) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(item.productName, color = MaterialTheme.colorScheme.onBackground)
            Text("${formatPrice(item.productPrice)} / ${item.saleUnit} × ${item.quantity}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(formatPrice(item.itemTotal), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
    }
}
