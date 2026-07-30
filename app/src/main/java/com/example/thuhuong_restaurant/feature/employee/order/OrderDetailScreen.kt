package com.example.thuhuong_restaurant.feature.employee.order

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.thuhuong_restaurant.core.common.formatPrice
import com.example.thuhuong_restaurant.core.model.OrderItemResponse
import com.example.thuhuong_restaurant.core.model.OrderResponse
import com.example.thuhuong_restaurant.core.model.OrderStatus
import com.example.thuhuong_restaurant.core.model.OrderSurchargeResponse
import com.example.thuhuong_restaurant.core.model.beerItems
import com.example.thuhuong_restaurant.core.model.bookingItems
import com.example.thuhuong_restaurant.core.model.foodItems
import com.example.thuhuong_restaurant.core.model.hasBookingItems
import com.example.thuhuong_restaurant.core.model.needsRefund
import com.example.thuhuong_restaurant.core.model.saleUnitLabel
import com.example.thuhuong_restaurant.core.ui.theme.ThAccentTeal
import com.example.thuhuong_restaurant.core.ui.theme.ThWarning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    onBack: () -> Unit,
    onNavigateToPayment: (String) -> Unit,
    viewModel: OrderDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.navigateToPayment) {
        if (state.navigateToPayment) {
            onNavigateToPayment(viewModel.orderId)
            viewModel.onNavigationConsumed()
        }
    }

    LaunchedEffect(state.navigateBack) {
        if (state.navigateBack) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.order?.tableName ?: "Chi tiết đơn") },
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
                state.error != null -> Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(state.error!!, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = viewModel::retry) { Text("Thử lại", color = MaterialTheme.colorScheme.primary) }
                }
                state.order != null -> OrderContent(
                    state = state,
                    onAddItemClick = viewModel::openProductPicker,
                    onIncrement = { item -> viewModel.adjustQuantity(item.id, item.quantity, 1) },
                    onDecrement = { item -> viewModel.adjustQuantity(item.id, item.quantity, -1) },
                    onRemove = { item -> viewModel.removeItem(item.id) },
                    onPaymentClick = viewModel::onPaymentClick,
                    onAddSurchargeClick = viewModel::openSurchargeDialog,
                    onRemoveSurcharge = viewModel::removeSurcharge,
                    onCancelClick = viewModel::openCancelDialog,
                    onConfirmRefund = viewModel::confirmRefund,
                    onToggleBeerList = viewModel::toggleBeerList,
                    onAddBeerClick = viewModel::openBeerDialog,
                )
            }
        }
    }

    if (state.showBeerDialog) {
        AddBeerDialog(
            quantity = state.beerQtyInput,
            isSubmitting = state.isAddingBeer,
            error = state.mutationError,
            onDigit = viewModel::appendBeerDigit,
            onClear = viewModel::clearBeerQty,
            onConfirm = viewModel::confirmAddBeer,
            onDismiss = viewModel::dismissBeerDialog,
        )
    }

    if (state.showProductPicker) {
        ModalBottomSheet(onDismissRequest = viewModel::closeProductPicker) {
            ProductPickerContent(
                picker = state.picker,
                onCategorySelected = viewModel::onPickerCategorySelected,
                onKeywordChange = viewModel::onPickerKeywordChange,
                onProductSelected = viewModel::selectProduct,
            )
        }
    }

    if (state.showSurchargeDialog) {
        AddSurchargeDialog(
            form = state.surchargeForm,
            error = state.surchargeError,
            isSubmitting = state.isSubmittingSurcharge,
            onNameChange = viewModel::updateSurchargeName,
            onUnitPriceChange = viewModel::updateSurchargeUnitPrice,
            onQuantityChange = viewModel::updateSurchargeQuantity,
            onReasonChange = viewModel::updateSurchargeReason,
            onConfirm = viewModel::confirmAddSurcharge,
            onDismiss = viewModel::dismissSurchargeDialog,
        )
    }

    if (state.showCancelDialog) {
        CancelOrderDialog(
            order = state.order,
            reason = state.cancelReason,
            error = state.cancelError,
            isCancelling = state.isCancelling,
            onReasonChange = viewModel::updateCancelReason,
            onConfirm = viewModel::confirmCancel,
            onDismiss = viewModel::dismissCancelDialog,
        )
    }
}

@Composable
private fun OrderContent(
    state: OrderDetailUiState,
    onAddItemClick: () -> Unit,
    onIncrement: (OrderItemResponse) -> Unit,
    onDecrement: (OrderItemResponse) -> Unit,
    onRemove: (OrderItemResponse) -> Unit,
    onPaymentClick: () -> Unit,
    onAddSurchargeClick: () -> Unit,
    onRemoveSurcharge: (String) -> Unit,
    onCancelClick: () -> Unit,
    onConfirmRefund: () -> Unit,
    onToggleBeerList: () -> Unit,
    onAddBeerClick: () -> Unit,
) {
    val order = state.order!!
    val needsRefund = order.needsRefund()
    val isOpen = order.status == OrderStatus.OPEN
    val canCancel = isOpen && !order.hasBookingItems() && !needsRefund
    val canEdit = isOpen && !state.isMutating

    // Ba nhóm tách riêng giống trang chi tiết đơn bên web: bia cốc gọi thành nhiều đợt nên để lẫn
    // vào danh sách món sẽ lấn hết chỗ, còn món từ đặt bàn thì không sửa được.
    val beerItems = order.beerItems
    val bookingItems = order.bookingItems
    val foodItems = order.foodItems
    val showBeerSection = state.beerProducts.isNotEmpty() || beerItems.isNotEmpty()

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.weight(1f)) {
            if (needsRefund) {
                RefundBanner(
                    amount = state.netRefund,
                    isRefunding = state.isRefunding,
                    error = state.refundError,
                    onConfirm = onConfirmRefund,
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (showBeerSection) {
                    item {
                        BeerSection(
                            items = beerItems,
                            expanded = state.showBeerList,
                            canEdit = canEdit,
                            canAdd = isOpen && state.beerProducts.isNotEmpty(),
                            onToggle = onToggleBeerList,
                            onAddClick = onAddBeerClick,
                            onIncrement = onIncrement,
                            onDecrement = onDecrement,
                            onRemove = onRemove,
                        )
                    }
                }

                if (bookingItems.isNotEmpty()) {
                    item { BookingItemsSection(items = bookingItems) }
                }

                if (foodItems.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "🍽️ MÓN ĂN · ${foodItems.size} món",
                            amount = foodItems.sumOf { it.itemTotal },
                            amountColor = MaterialTheme.colorScheme.primary,
                        )
                    }
                    items(foodItems, key = { it.id }) { item ->
                        OrderItemRow(
                            item = item,
                            enabled = canEdit,
                            onIncrement = { onIncrement(item) },
                            onDecrement = { onDecrement(item) },
                            onRemove = { onRemove(item) },
                        )
                    }
                }

                if (order.items.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text("🍽️", fontSize = 32.sp)
                            Spacer(Modifier.height(8.dp))
                            Text("Chưa có món nào", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                if (order.surcharges.isNotEmpty() || isOpen) {
                    item {
                        SurchargeSection(
                            surcharges = order.surcharges,
                            canEdit = isOpen,
                            onAddClick = onAddSurchargeClick,
                            onRemove = onRemoveSurcharge,
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Tổng cộng", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
                Text(
                    formatPrice(order.totalAmount),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.height(12.dp))
            if (order.status == OrderStatus.OPEN) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedButton(
                        onClick = onAddItemClick,
                        shape = MaterialTheme.shapes.small,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Thêm món")
                    }
                    if (!needsRefund) {
                        Button(
                            onClick = onPaymentClick,
                            enabled = state.trueRemaining > 0,
                            shape = MaterialTheme.shapes.small,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Thanh toán")
                        }
                    }
                }
                if (canCancel) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onCancelClick, modifier = Modifier.fillMaxWidth()) {
                        Text("Hủy đơn", color = MaterialTheme.colorScheme.error)
                    }
                }
            } else {
                Text(
                    "Đơn đã ${if (order.status == OrderStatus.PAID) "thanh toán" else "hủy"}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    amount: Double,
    amountColor: androidx.compose.ui.graphics.Color,
    note: String? = null,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        if (note != null) {
            Text(
                note,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = MaterialTheme.typography.bodySmall.fontSize,
            )
            Spacer(Modifier.width(8.dp))
        }
        if (amount > 0) {
            Text(
                formatPrice(amount),
                fontWeight = FontWeight.Bold,
                color = amountColor,
                fontSize = MaterialTheme.typography.bodySmall.fontSize,
            )
        }
    }
}

/**
 * Bia cốc: khách gọi thành nhiều đợt trong bữa, mỗi đợt một ô riêng để nhân viên đối chiếu được
 * từng lần gọi. Mặc định thu gọn vì một bữa có thể lên tới cả chục đợt.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BeerSection(
    items: List<OrderItemResponse>,
    expanded: Boolean,
    canEdit: Boolean,
    canAdd: Boolean,
    onToggle: () -> Unit,
    onAddClick: () -> Unit,
    onIncrement: (OrderItemResponse) -> Unit,
    onDecrement: (OrderItemResponse) -> Unit,
    onRemove: (OrderItemResponse) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(
            title = "🍺 BIA CỐC" + if (items.isNotEmpty()) " · ${items.size} đợt" else "",
            amount = items.sumOf { it.itemTotal },
            amountColor = MaterialTheme.colorScheme.primary,
            note = if (items.isNotEmpty()) "${items.sumOf { it.quantity }} cốc" else null,
        )

        if (expanded && items.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items.forEach { item ->
                    BeerRoundChip(
                        item = item,
                        canEdit = canEdit,
                        onIncrement = { onIncrement(item) },
                        onDecrement = { onDecrement(item) },
                        onRemove = { onRemove(item) },
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (canAdd) {
                OutlinedButton(
                    onClick = onAddClick,
                    shape = MaterialTheme.shapes.small,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Thêm đợt", color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.weight(1f))
            if (items.isNotEmpty()) {
                TextButton(onClick = onToggle) {
                    Text(
                        if (expanded) "▲ Thu gọn" else "▼ ${items.size} đợt",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = MaterialTheme.typography.bodySmall.fontSize,
                    )
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
    }
}

/** Một đợt bia: giảm / số cốc / tăng, và ✕ để bỏ cả đợt. */
@Composable
private fun BeerRoundChip(
    item: OrderItemResponse,
    canEdit: Boolean,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onRemove: () -> Unit,
) {
    Card(
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        if (canEdit) {
            Row(
                modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onDecrement,
                    enabled = item.quantity > 1,
                    modifier = Modifier.size(34.dp),
                ) {
                    Icon(Icons.Filled.Remove, contentDescription = "Bớt một cốc", modifier = Modifier.size(16.dp))
                }
                Text(
                    "${item.quantity}",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.widthIn(min = 24.dp),
                )
                IconButton(onClick = onIncrement, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Filled.Add, contentDescription = "Thêm một cốc", modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onRemove, modifier = Modifier.size(34.dp)) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Bỏ cả đợt",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        } else {
            Text(
                "${item.quantity}",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
    }
}

/** Món khách đã đặt trước qua booking — chỉ đọc, sửa phải vào phần đặt bàn. */
@Composable
private fun BookingItemsSection(items: List<OrderItemResponse>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(
            title = "📋 TỪ ĐẶT BÀN · ${items.size} món",
            amount = items.sumOf { it.itemTotal },
            amountColor = ThAccentTeal,
        )
        items.forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.productName, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground)
                    Text(
                        "${formatPrice(item.productPrice)} / ${saleUnitLabel(item.saleUnit)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = MaterialTheme.typography.bodySmall.fontSize,
                    )
                }
                Text(
                    "×${item.quantity}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = MaterialTheme.typography.bodySmall.fontSize,
                )
                Spacer(Modifier.width(10.dp))
                Text(formatPrice(item.itemTotal), fontWeight = FontWeight.Bold, color = ThAccentTeal)
                Spacer(Modifier.width(8.dp))
                Icon(
                    Icons.Filled.Lock,
                    contentDescription = "Món từ đặt bàn, không sửa được",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
    }
}

/** Bàn phím số để thêm nhanh một đợt bia — nhân viên hay phải nhập lúc đang bê đồ. */
@Composable
private fun AddBeerDialog(
    quantity: String,
    isSubmitting: Boolean,
    error: String?,
    onDigit: (String) -> Unit,
    onClear: () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val qty = quantity.toIntOrNull() ?: 0
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🍺 Thêm đợt bia cốc") },
        text = {
            Column {
                Text(
                    "Nhập số cốc rồi bấm Thêm",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = MaterialTheme.typography.bodySmall.fontSize,
                )
                Spacer(Modifier.height(12.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Text(
                            quantity.ifEmpty { "0" },
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("cốc", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(12.dp))
                listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("C", "0", ""),
                ).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        row.forEach { key ->
                            if (key.isEmpty()) {
                                Spacer(Modifier.weight(1f))
                            } else {
                                OutlinedButton(
                                    onClick = { if (key == "C") onClear() else onDigit(key) },
                                    shape = MaterialTheme.shapes.small,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    modifier = Modifier.weight(1f).height(48.dp),
                                ) {
                                    Text(
                                        key,
                                        fontWeight = FontWeight.Bold,
                                        color = if (key == "C") MaterialTheme.colorScheme.onSurfaceVariant
                                        else MaterialTheme.colorScheme.onBackground,
                                    )
                                }
                            }
                        }
                    }
                }
                if (error != null) {
                    Text(error, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isSubmitting && qty >= 1) {
                Text(
                    if (isSubmitting) "Đang thêm..." else "Thêm $qty cốc",
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Đóng") }
        },
    )
}

@Composable
private fun RefundBanner(amount: Double, isRefunding: Boolean, error: String?, onConfirm: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ThWarning.copy(alpha = 0.15f)),
        modifier = Modifier.fillMaxWidth().padding(16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("💰 Cần hoàn ${formatPrice(amount)} cho khách", color = ThWarning, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            if (error != null) {
                Text(error, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
            }
            Button(
                onClick = onConfirm,
                enabled = !isRefunding,
                shape = MaterialTheme.shapes.small,
                colors = ButtonDefaults.buttonColors(containerColor = ThWarning),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (isRefunding) "Đang xử lý..." else "✓ Xác nhận đã hoàn tiền cho khách")
            }
        }
    }
}

@Composable
private fun SurchargeSection(
    surcharges: List<OrderSurchargeResponse>,
    canEdit: Boolean,
    onAddClick: () -> Unit,
    onRemove: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Phụ thu", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(8.dp))
        surcharges.forEach { surcharge ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("⚠️ ${surcharge.name}", color = MaterialTheme.colorScheme.onBackground)
                    Text(
                        "${formatPrice(surcharge.unitPrice)} × ${surcharge.quantity}" +
                            (surcharge.reason?.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = MaterialTheme.typography.bodySmall.fontSize,
                    )
                }
                Text(formatPrice(surcharge.amount), color = ThWarning, fontWeight = FontWeight.SemiBold)
                if (canEdit) {
                    IconButton(onClick = { onRemove(surcharge.id) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Xóa phụ thu", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
        if (canEdit) {
            Spacer(Modifier.height(4.dp))
            TextButton(onClick = onAddClick) {
                Text("+ Thêm phụ thu", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSurchargeDialog(
    form: SurchargeFormState,
    error: String?,
    isSubmitting: Boolean,
    onNameChange: (String) -> Unit,
    onUnitPriceChange: (String) -> Unit,
    onQuantityChange: (Int) -> Unit,
    onReasonChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thêm phụ thu") },
        text = {
            Column {
                OutlinedTextField(
                    value = form.name,
                    onValueChange = onNameChange,
                    label = { Text("Tên phụ thu") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = form.unitPrice,
                    onValueChange = onUnitPriceChange,
                    label = { Text("Đơn giá") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onQuantityChange(form.quantity - 1) }) {
                        Icon(Icons.Filled.Remove, contentDescription = "Giảm")
                    }
                    Text("${form.quantity}", fontWeight = FontWeight.SemiBold)
                    IconButton(onClick = { onQuantityChange(form.quantity + 1) }) {
                        Icon(Icons.Filled.Add, contentDescription = "Tăng")
                    }
                }
                OutlinedTextField(
                    value = form.reason,
                    onValueChange = onReasonChange,
                    label = { Text("Lý do (tùy chọn)") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth(),
                )
                val price = form.unitPrice.toDoubleOrNull() ?: 0.0
                Spacer(Modifier.height(8.dp))
                Text("Thành tiền: ${formatPrice(price * form.quantity)}", color = MaterialTheme.colorScheme.primary)
                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(error, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isSubmitting) {
                Text("Xác nhận thêm phụ thu", color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CancelOrderDialog(
    order: OrderResponse?,
    reason: String,
    error: String?,
    isCancelling: Boolean,
    onReasonChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Hủy đơn hàng?") },
        text = {
            Column {
                if (order?.items?.isNotEmpty() == true) {
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
                if (error != null) {
                    Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isCancelling) {
                Text("Hủy đơn", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Đóng") }
        },
    )
}

@Composable
private fun OrderItemRow(
    item: OrderItemResponse,
    enabled: Boolean,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onRemove: () -> Unit,
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.productName, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground)
                Text(
                    "${formatPrice(item.productPrice)} / ${saleUnitLabel(item.saleUnit)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = MaterialTheme.typography.bodySmall.fontSize,
                )
            }
            IconButton(onClick = onDecrement, enabled = enabled) {
                Icon(Icons.Filled.Remove, contentDescription = "Giảm")
            }
            Text("${item.quantity}", fontWeight = FontWeight.SemiBold)
            IconButton(onClick = onIncrement, enabled = enabled) {
                Icon(Icons.Filled.Add, contentDescription = "Tăng")
            }
            IconButton(onClick = onRemove, enabled = enabled) {
                Icon(Icons.Filled.Delete, contentDescription = "Xóa", tint = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.width(8.dp))
            Text(
                formatPrice(item.itemTotal),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
