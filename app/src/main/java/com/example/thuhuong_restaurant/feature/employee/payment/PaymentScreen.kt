package com.example.thuhuong_restaurant.feature.employee.payment

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.thuhuong_restaurant.core.common.formatPrice
import com.example.thuhuong_restaurant.core.model.PaymentMethod
import com.example.thuhuong_restaurant.core.model.PaymentStatus
import com.example.thuhuong_restaurant.core.model.label

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    onBack: () -> Unit,
    onPaymentCompleted: () -> Unit,
    viewModel: PaymentViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Thanh toán") },
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
                state.isLoadingOrder -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary,
                )
                state.loadError != null -> Text(
                    state.loadError!!,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center),
                )
                state.payment?.status == PaymentStatus.COMPLETED -> SuccessContent(onDone = onPaymentCompleted)
                state.payment != null -> PendingPaymentContent(
                    method = state.payment!!.paymentMethod,
                    amount = state.payment!!.amount,
                    vietQrUrl = state.payment!!.vietQrUrl,
                    isConfirming = state.isConfirming,
                    confirmError = state.confirmError,
                    isChangingMethod = state.isChangingMethod,
                    changeMethodError = state.changeMethodError,
                    onConfirm = viewModel::confirmManually,
                    onSwitchMethod = viewModel::changeMethod,
                )
                else -> MethodSelectionContent(
                    remaining = state.remaining ?: 0.0,
                    amountInput = state.partialAmountInput,
                    onAmountChange = viewModel::updatePartialAmount,
                    selectedMethod = state.selectedMethod,
                    isCreating = state.isCreating,
                    createError = state.createError,
                    onSelectMethod = viewModel::selectMethod,
                    onConfirm = viewModel::createPayment,
                )
            }
        }
    }
}

@Composable
private fun MethodSelectionContent(
    remaining: Double,
    amountInput: String,
    onAmountChange: (String) -> Unit,
    selectedMethod: PaymentMethod?,
    isCreating: Boolean,
    createError: String?,
    onSelectMethod: (PaymentMethod) -> Unit,
    onConfirm: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text("Số tiền cần thu", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            formatPrice(remaining),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = amountInput,
            onValueChange = onAmountChange,
            label = { Text("Số tiền thu (có thể thu một phần)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = MaterialTheme.shapes.small,
            colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(20.dp))
        Text("Chọn phương thức thanh toán", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(12.dp))
        MethodCard(
            label = "Tiền mặt",
            selected = selectedMethod == PaymentMethod.CASH,
            onClick = { onSelectMethod(PaymentMethod.CASH) },
        )
        Spacer(Modifier.height(10.dp))
        MethodCard(
            label = "Chuyển khoản (VietQR)",
            selected = selectedMethod == PaymentMethod.BANK_TRANSFER,
            onClick = { onSelectMethod(PaymentMethod.BANK_TRANSFER) },
        )

        if (createError != null) {
            Spacer(Modifier.height(12.dp))
            Text(createError, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.weight(1f))
        Button(
            onClick = onConfirm,
            enabled = selectedMethod != null && !isCreating,
            shape = MaterialTheme.shapes.small,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (isCreating) {
                CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
            } else {
                Text("Tiếp tục")
            }
        }
    }
}

@Composable
private fun MethodCard(label: String, selected: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.background,
        ),
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            label,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
private fun PaymentMethodSwitcher(
    current: PaymentMethod,
    isChanging: Boolean,
    onSelect: (PaymentMethod) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        MethodSegment(
            label = "Tiền mặt",
            selected = current == PaymentMethod.CASH,
            enabled = !isChanging,
            onClick = { onSelect(PaymentMethod.CASH) },
            modifier = Modifier.weight(1f),
        )
        MethodSegment(
            label = "Chuyển khoản",
            selected = current == PaymentMethod.BANK_TRANSFER,
            enabled = !isChanging,
            onClick = { onSelect(PaymentMethod.BANK_TRANSFER) },
            modifier = Modifier.weight(1f),
        )
    }
    if (isChanging) {
        Spacer(Modifier.height(8.dp))
        CircularProgressIndicator(modifier = Modifier.height(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun MethodSegment(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background)
            .clickable(enabled = enabled && !selected, onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PendingPaymentContent(
    method: PaymentMethod,
    amount: Double,
    vietQrUrl: String?,
    isConfirming: Boolean,
    confirmError: String?,
    isChangingMethod: Boolean,
    changeMethodError: String?,
    onConfirm: () -> Unit,
    onSwitchMethod: (PaymentMethod) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Số tiền cần thu", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            formatPrice(amount),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(16.dp))
        PaymentMethodSwitcher(
            current = method,
            isChanging = isChangingMethod,
            onSelect = onSwitchMethod,
        )
        if (changeMethodError != null) {
            Spacer(Modifier.height(8.dp))
            Text(changeMethodError, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(20.dp))

        if (method == PaymentMethod.BANK_TRANSFER && vietQrUrl != null) {
            AsyncImage(
                model = vietQrUrl,
                contentDescription = "Mã QR chuyển khoản",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Đang chờ khách chuyển khoản...",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
        }

        if (confirmError != null) {
            Text(confirmError, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.weight(1f))
        Button(
            onClick = onConfirm,
            enabled = !isConfirming,
            shape = MaterialTheme.shapes.small,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (isConfirming) {
                CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
            } else {
                Text(if (method == PaymentMethod.CASH) "Xác nhận đã thu tiền" else "Xác nhận thủ công đã nhận tiền")
            }
        }
    }
}

@Composable
private fun SuccessContent(onDone: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.height(72.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Thanh toán thành công",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(24.dp))
        TextButton(onClick = onDone) {
            Text("Về bản đồ bàn", color = MaterialTheme.colorScheme.primary)
        }
    }
}
