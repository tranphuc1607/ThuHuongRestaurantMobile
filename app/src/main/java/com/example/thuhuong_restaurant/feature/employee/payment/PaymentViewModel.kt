package com.example.thuhuong_restaurant.feature.employee.payment

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thuhuong_restaurant.core.common.ApiResult
import com.example.thuhuong_restaurant.core.model.PaymentMethod
import com.example.thuhuong_restaurant.core.model.PaymentResponse
import com.example.thuhuong_restaurant.core.model.PaymentStatus
import com.example.thuhuong_restaurant.feature.employee.order.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PaymentUiState(
    val orderTotal: Double? = null,
    val prepaidAmount: Double = 0.0,
    /** the true amount still owed = orderTotal - prepaidAmount - sum of already-completed payments. */
    val remaining: Double? = null,
    val hasCompletedPayment: Boolean = false,
    val isLoadingOrder: Boolean = true,
    val loadError: String? = null,
    val selectedMethod: PaymentMethod? = null,
    val partialAmountInput: String = "",
    val isCreating: Boolean = false,
    val createError: String? = null,
    val payment: PaymentResponse? = null,
    val isConfirming: Boolean = false,
    val confirmError: String? = null,
    val isChangingMethod: Boolean = false,
    val changeMethodError: String? = null,
)

@HiltViewModel
class PaymentViewModel @Inject constructor(
    private val paymentRepository: PaymentRepository,
    private val orderRepository: OrderRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val orderId: String = checkNotNull(savedStateHandle["orderId"])

    private val _uiState = MutableStateFlow(PaymentUiState())
    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()

    private var pollJob: Job? = null

    init {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingOrder = true, loadError = null)
            when (val orderResult = orderRepository.getOrder(orderId)) {
                is ApiResult.Success -> {
                    val order = orderResult.data
                    val sumCompleted = when (val paymentsResult = paymentRepository.getAllByOrder(orderId)) {
                        is ApiResult.Success -> paymentsResult.data
                            .filter { it.paymentMethod != PaymentMethod.REFUND && it.status == PaymentStatus.COMPLETED }
                            .sumOf { it.amount }
                        is ApiResult.Failure -> 0.0
                    }
                    val remaining = (order.totalAmount - order.prepaidAmount - sumCompleted).coerceAtLeast(0.0)
                    val hasCompleted = sumCompleted > 0
                    _uiState.value = _uiState.value.copy(
                        orderTotal = order.totalAmount,
                        prepaidAmount = order.prepaidAmount,
                        remaining = remaining,
                        hasCompletedPayment = hasCompleted,
                        partialAmountInput = remaining.toPlainInput(),
                        isLoadingOrder = false,
                    )
                    loadExistingPayment(remaining, hasCompleted)
                }
                is ApiResult.Failure -> _uiState.value = _uiState.value.copy(
                    isLoadingOrder = false,
                    loadError = orderResult.message,
                )
            }
        }
    }

    // Đơn có thể đã có 1 payment PENDING từ trước (vd. nhân viên thoát ra rồi vào lại) —
    // phải hiển thị đúng trạng thái đó thay vì cho chọn phương thức lại từ đầu. Nếu đơn hàng
    // đã đổi tổng tiền từ lúc tạo payment (khách gọi thêm món) thì đồng bộ lại amount trước khi hiển thị —
    // NHƯNG chỉ khi chưa có payment nào COMPLETED, để không phá vỡ một khoản thanh toán chia nhiều lần đang dở.
    private suspend fun loadExistingPayment(remaining: Double, hasCompletedPayment: Boolean) {
        when (val result = paymentRepository.getPaymentByOrder(orderId)) {
            is ApiResult.Success -> {
                if (result.data.status != PaymentStatus.PENDING) return
                var payment = result.data
                if (!hasCompletedPayment && kotlin.math.abs(payment.amount - remaining) > 0.5) {
                    when (val updateResult = paymentRepository.updateAmount(payment.id, remaining)) {
                        is ApiResult.Success -> payment = updateResult.data
                        is ApiResult.Failure -> Unit // giữ số tiền cũ nếu đồng bộ thất bại, nhân viên vẫn xác nhận được
                    }
                }
                _uiState.value = _uiState.value.copy(payment = payment)
                if (payment.paymentMethod == PaymentMethod.BANK_TRANSFER) startPolling(payment.id)
            }
            is ApiResult.Failure -> Unit // chưa có payment nào cho đơn này — bỏ qua, hiển thị chọn phương thức
        }
    }

    fun selectMethod(method: PaymentMethod) {
        _uiState.value = _uiState.value.copy(selectedMethod = method)
    }

    fun updatePartialAmount(input: String) {
        _uiState.value = _uiState.value.copy(partialAmountInput = input, createError = null)
    }

    fun createPayment() {
        val method = _uiState.value.selectedMethod ?: return
        val remaining = _uiState.value.remaining ?: return
        val amount = _uiState.value.partialAmountInput.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            _uiState.value = _uiState.value.copy(createError = "Số tiền không hợp lệ")
            return
        }
        if (amount > remaining + 0.5) {
            _uiState.value = _uiState.value.copy(createError = "Số tiền vượt quá phần còn lại")
            return
        }
        // omit amount entirely when it equals the full remaining — matches backend default (null = full remaining)
        val amountToSend = if (kotlin.math.abs(amount - remaining) < 0.5) null else amount
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreating = true, createError = null)
            when (val result = paymentRepository.createPayment(orderId, method, amountToSend)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(isCreating = false, payment = result.data)
                    if (method == PaymentMethod.BANK_TRANSFER) startPolling(result.data.id)
                }
                is ApiResult.Failure -> _uiState.value = _uiState.value.copy(isCreating = false, createError = result.message)
            }
        }
    }

    private fun startPolling(paymentId: String) {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (_uiState.value.payment?.status != PaymentStatus.COMPLETED) {
                delay(3000)
                when (val result = paymentRepository.getPayment(paymentId)) {
                    is ApiResult.Success -> _uiState.value = _uiState.value.copy(payment = result.data)
                    is ApiResult.Failure -> Unit
                }
            }
        }
    }

    fun changeMethod(newMethod: PaymentMethod) {
        val payment = _uiState.value.payment ?: return
        if (payment.paymentMethod == newMethod || _uiState.value.isChangingMethod) return
        pollJob?.cancel()
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isChangingMethod = true, changeMethodError = null)
            when (val result = paymentRepository.changeMethod(payment.id, newMethod)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(isChangingMethod = false, payment = result.data)
                    if (newMethod == PaymentMethod.BANK_TRANSFER) startPolling(result.data.id)
                }
                is ApiResult.Failure -> _uiState.value = _uiState.value.copy(isChangingMethod = false, changeMethodError = result.message)
            }
        }
    }

    fun confirmManually() {
        val payment = _uiState.value.payment ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isConfirming = true, confirmError = null)
            when (val result = paymentRepository.confirmPayment(payment.id)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(isConfirming = false, payment = result.data)
                    pollJob?.cancel()
                }
                is ApiResult.Failure -> _uiState.value = _uiState.value.copy(isConfirming = false, confirmError = result.message)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollJob?.cancel()
    }
}

private fun Double.toPlainInput(): String =
    if (this == Math.floor(this)) this.toLong().toString() else this.toString()
