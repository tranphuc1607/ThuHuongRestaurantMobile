package com.example.thuhuong_restaurant.feature.employee.payment

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import com.example.thuhuong_restaurant.core.common.formatDateTime
import com.example.thuhuong_restaurant.core.common.formatPrice
import com.example.thuhuong_restaurant.core.model.PaymentMethod
import com.example.thuhuong_restaurant.core.model.PaymentResponse
import com.example.thuhuong_restaurant.core.model.PaymentStatus
import com.example.thuhuong_restaurant.core.model.label
import com.example.thuhuong_restaurant.core.ui.components.CategoryPill
import com.example.thuhuong_restaurant.core.ui.theme.ThSuccess
import com.example.thuhuong_restaurant.core.ui.theme.ThWarning
import com.example.thuhuong_restaurant.feature.employee.DateRangeBar
import com.example.thuhuong_restaurant.feature.employee.EmployeeTabRow

private data class LedgerFilterOption(val value: PaymentStatus?, val label: String)

private val FILTERS = listOf(
    LedgerFilterOption(null, "Tất cả"),
    LedgerFilterOption(PaymentStatus.PENDING, "Chờ xác nhận"),
    LedgerFilterOption(PaymentStatus.COMPLETED, "Hoàn thành"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayPaymentsScreen(
    navController: NavHostController,
    viewModel: TodayPaymentsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    val visiblePayments = state.payments.filter { !(state.hideExpiredBankTransfer && it.isExpiredBankTransfer()) }
    val completed = state.payments.filter { it.status == PaymentStatus.COMPLETED }
    val totalCollected = completed.filter { it.paymentMethod != PaymentMethod.REFUND }.sumOf { it.amount } -
        completed.filter { it.paymentMethod == PaymentMethod.REFUND }.sumOf { it.amount }
    val pendingList = state.payments.filter { it.status == PaymentStatus.PENDING }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(if (state.dateRange.isToday) "Thu ngân hôm nay" else "Thu ngân") },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                )
                EmployeeTabRow(navController)
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SummaryTile("Giao dịch", state.payments.size.toString(), Modifier.weight(1f))
                SummaryTile("Đã thu", formatPrice(totalCollected), Modifier.weight(1f))
                SummaryTile("Chờ xác nhận", pendingList.size.toString(), Modifier.weight(1f))
            }

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
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
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 12.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "Ẩn CK hết hạn (>30 phút)",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = viewModel::toggleHideExpired) {
                    Text(
                        if (state.hideExpiredBankTransfer) "Hiện" else "Ẩn",
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

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
                    visiblePayments.isEmpty() -> Text(
                        "Chưa có giao dịch nào hôm nay",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center),
                    )
                    else -> LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(visiblePayments, key = { it.id }) { payment ->
                            PaymentRow(
                                payment = payment,
                                isConfirming = state.confirmingId == payment.id,
                                onConfirm = { viewModel.confirmPayment(payment.id) },
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
}

@Composable
private fun SummaryTile(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = MaterialTheme.typography.bodySmall.fontSize)
            Text(value, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PaymentRow(payment: PaymentResponse, isConfirming: Boolean, onConfirm: () -> Unit) {
    val isRefund = payment.paymentMethod == PaymentMethod.REFUND
    val statusColor = when (payment.status) {
        PaymentStatus.COMPLETED -> ThSuccess
        PaymentStatus.PENDING -> ThWarning
        else -> MaterialTheme.colorScheme.error
    }

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(payment.tableName ?: "Không có bàn", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Text(payment.status.name, color = statusColor, fontWeight = FontWeight.SemiBold)
            }
            Text(payment.paymentMethod.label(), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatDateTime(payment.createdAt), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(
                (if (isRefund) "− " else "") + formatPrice(payment.amount),
                color = if (isRefund) ThWarning else MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
            if (payment.status == PaymentStatus.PENDING) {
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onConfirm, enabled = !isConfirming) {
                    Text(if (isConfirming) "Đang xác nhận..." else "Xác nhận", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
