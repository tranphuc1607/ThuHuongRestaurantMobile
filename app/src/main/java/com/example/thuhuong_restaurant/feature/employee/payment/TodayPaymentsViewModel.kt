package com.example.thuhuong_restaurant.feature.employee.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thuhuong_restaurant.core.common.ApiResult
import com.example.thuhuong_restaurant.core.common.DateRangeState
import com.example.thuhuong_restaurant.core.model.PaymentMethod
import com.example.thuhuong_restaurant.core.model.PaymentResponse
import com.example.thuhuong_restaurant.core.model.PaymentStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

private const val EXPIRE_MS = 30 * 60 * 1000L

data class TodayPaymentsUiState(
    val statusFilter: PaymentStatus? = null,
    val dateRange: DateRangeState = DateRangeState(),
    val payments: List<PaymentResponse> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val page: Int = 0,
    val hasMore: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hideExpiredBankTransfer: Boolean = true,
    val confirmingId: String? = null,
    val confirmError: String? = null,
)

@HiltViewModel
class TodayPaymentsViewModel @Inject constructor(
    private val repository: PaymentRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TodayPaymentsUiState())
    val uiState: StateFlow<TodayPaymentsUiState> = _uiState.asStateFlow()

    init {
        load(reset = true)
    }

    fun selectFilter(status: PaymentStatus?) {
        if (status == _uiState.value.statusFilter) return
        _uiState.value = _uiState.value.copy(statusFilter = status)
        load(reset = true)
    }

    fun updateDateRange(range: DateRangeState) {
        _uiState.value = _uiState.value.copy(dateRange = range)
        load(reset = true)
    }

    fun toggleHideExpired() {
        _uiState.value = _uiState.value.copy(hideExpiredBankTransfer = !_uiState.value.hideExpiredBankTransfer)
    }

    fun retry() = load(reset = true)

    fun loadMore() {
        if (!_uiState.value.hasMore || _uiState.value.isLoadingMore) return
        load(reset = false)
    }

    private fun load(reset: Boolean) {
        viewModelScope.launch {
            val targetPage = if (reset) 0 else _uiState.value.page + 1
            _uiState.value = _uiState.value.copy(
                isLoading = reset && _uiState.value.payments.isEmpty(),
                isLoadingMore = !reset,
                error = null,
            )
            val range = _uiState.value.dateRange
            val result = if (range.isToday) {
                repository.getToday(_uiState.value.statusFilter?.name, targetPage)
            } else {
                repository.getAll(_uiState.value.statusFilter?.name, range.fromIso, range.toIso, targetPage)
            }
            when (result) {
                is ApiResult.Success -> {
                    val merged = if (reset) result.data.content else _uiState.value.payments + result.data.content
                    _uiState.value = _uiState.value.copy(
                        payments = merged,
                        page = targetPage,
                        hasMore = targetPage < result.data.totalPages - 1,
                        isLoading = false,
                        isLoadingMore = false,
                    )
                }
                is ApiResult.Failure -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    error = if (_uiState.value.payments.isEmpty()) result.message else null,
                )
            }
        }
    }

    fun confirmPayment(id: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(confirmingId = id, confirmError = null)
            when (val result = repository.confirmPayment(id)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        confirmingId = null,
                        payments = _uiState.value.payments.map { if (it.id == id) result.data else it },
                    )
                }
                is ApiResult.Failure -> _uiState.value = _uiState.value.copy(confirmingId = null, confirmError = result.message)
            }
        }
    }
}

fun PaymentResponse.isExpiredBankTransfer(): Boolean {
    if (paymentMethod != PaymentMethod.BANK_TRANSFER || status != PaymentStatus.PENDING) return false
    val created = createdAt ?: return false
    return try {
        val createdMillis = Instant.parse(created).toEpochMilli()
        System.currentTimeMillis() - createdMillis > EXPIRE_MS
    } catch (e: Exception) {
        false
    }
}
