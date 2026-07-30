package com.example.thuhuong_restaurant.feature.employee.orderlist

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.thuhuong_restaurant.core.common.formatPrice
import com.example.thuhuong_restaurant.core.model.OrderResponse
import com.example.thuhuong_restaurant.core.model.OrderStatus
import com.example.thuhuong_restaurant.core.model.hasBookingItems
import com.example.thuhuong_restaurant.core.model.needsRefund
import com.example.thuhuong_restaurant.core.navigation.Routes
import com.example.thuhuong_restaurant.core.ui.components.CategoryPill
import com.example.thuhuong_restaurant.core.ui.theme.ThSuccess
import com.example.thuhuong_restaurant.core.ui.theme.ThWarning
import com.example.thuhuong_restaurant.feature.employee.DateRangeBar
import com.example.thuhuong_restaurant.feature.employee.EmployeeTabRow

private data class StatusFilterOption(val value: String?, val label: String)

private val FILTERS = listOf(
    StatusFilterOption("OPEN", "Đang mở"),
    StatusFilterOption("PAID", "Đã thanh toán"),
    StatusFilterOption("CANCELLED", "Đã hủy"),
    StatusFilterOption(null, "Tất cả"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderListScreen(
    navController: NavHostController,
    onManageOrder: (String) -> Unit,
    onPayOrder: (String) -> Unit,
    viewModel: OrderListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Danh sách đơn hàng") },
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
                    state.orders.isEmpty() -> Text(
                        "Không có đơn hàng nào",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center),
                    )
                    else -> LazyColumn(
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(state.orders, key = { it.id }) { order ->
                            OrderRow(
                                order = order,
                                onManage = { onManageOrder(order.id) },
                                onPay = { onPayOrder(order.id) },
                                onCancel = { viewModel.openCancelDialog(order) },
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

    state.cancelTarget?.let { order ->
        CancelOrderDialog(
            order = order,
            reason = state.cancelReason,
            onReasonChange = viewModel::updateCancelReason,
            error = state.cancelError,
            isCancelling = state.isCancelling,
            onConfirm = viewModel::confirmCancel,
            onDismiss = viewModel::dismissCancelDialog,
        )
    }
}

@Composable
private fun OrderRow(
    order: OrderResponse,
    onManage: () -> Unit,
    onPay: () -> Unit,
    onCancel: () -> Unit,
) {
    val statusColor = when (order.status) {
        OrderStatus.OPEN -> MaterialTheme.colorScheme.primary
        OrderStatus.PAID -> ThSuccess
        OrderStatus.CANCELLED -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val statusLabel = when (order.status) {
        OrderStatus.OPEN -> "Đang mở"
        OrderStatus.PAID -> "Đã thanh toán"
        OrderStatus.CANCELLED -> "Đã hủy"
    }
    val needsRefund = order.needsRefund()

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
        border = BorderStroke(1.dp, if (needsRefund) ThWarning else MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    order.tableName ?: "Không có bàn",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    if (needsRefund) "💰 Cần hoàn" else statusLabel,
                    fontWeight = FontWeight.SemiBold,
                    color = if (needsRefund) ThWarning else statusColor,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(formatPrice(order.totalAmount), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)

            if (order.status == OrderStatus.OPEN) {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onManage, shape = MaterialTheme.shapes.small, modifier = Modifier.weight(1f)) {
                        Text("Quản lý")
                    }
                    if (order.totalAmount > 0 && !needsRefund) {
                        OutlinedButton(onClick = onPay, shape = MaterialTheme.shapes.small, modifier = Modifier.weight(1f)) {
                            Text("Thanh toán")
                        }
                    }
                    if (!needsRefund) {
                        TextButton(onClick = onCancel) {
                            Text("Hủy", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            } else {
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = onManage) { Text("Xem chi tiết", color = MaterialTheme.colorScheme.primary) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CancelOrderDialog(
    order: OrderResponse,
    reason: String,
    onReasonChange: (String) -> Unit,
    error: String?,
    isCancelling: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val blockedByBooking = order.hasBookingItems()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Hủy đơn ${order.tableName ?: ""}") },
        text = {
            Column {
                if (blockedByBooking) {
                    Text(
                        "Đơn này được chuyển từ đặt bàn nên không thể hủy.",
                        color = MaterialTheme.colorScheme.error,
                    )
                } else {
                    if (order.items.isNotEmpty()) {
                        OutlinedTextField(
                            value = reason,
                            onValueChange = onReasonChange,
                            label = { Text("Lý do hủy") },
                            shape = MaterialTheme.shapes.small,
                            colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = MaterialTheme.colorScheme.outline),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        Text("Xác nhận hủy đơn này?", color = MaterialTheme.colorScheme.onBackground)
                    }
                }
                if (error != null) {
                    Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
                }
            }
        },
        confirmButton = {
            if (!blockedByBooking) {
                TextButton(onClick = onConfirm, enabled = !isCancelling) {
                    Text("Hủy đơn", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Đóng") }
        },
    )
}
