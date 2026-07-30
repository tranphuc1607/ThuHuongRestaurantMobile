package com.example.thuhuong_restaurant.feature.employee.orderlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thuhuong_restaurant.core.common.ApiResult
import com.example.thuhuong_restaurant.core.common.DateRangeState
import com.example.thuhuong_restaurant.core.model.OrderResponse
import com.example.thuhuong_restaurant.feature.employee.order.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OrderListUiState(
    val statusFilter: String? = "OPEN",
    val dateRange: DateRangeState = DateRangeState(),
    val orders: List<OrderResponse> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val page: Int = 0,
    val hasMore: Boolean = false,
    val isLoadingMore: Boolean = false,
    val cancelTarget: OrderResponse? = null,
    val cancelReason: String = "",
    val isCancelling: Boolean = false,
    val cancelError: String? = null,
)

@HiltViewModel
class OrderListViewModel @Inject constructor(
    private val repository: OrderRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrderListUiState())
    val uiState: StateFlow<OrderListUiState> = _uiState.asStateFlow()

    init {
        load(reset = true)
    }

    fun selectFilter(status: String?) {
        if (status == _uiState.value.statusFilter) return
        _uiState.value = _uiState.value.copy(statusFilter = status)
        load(reset = true)
    }

    fun updateDateRange(range: DateRangeState) {
        _uiState.value = _uiState.value.copy(dateRange = range)
        load(reset = true)
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
                isLoading = reset && _uiState.value.orders.isEmpty(),
                isLoadingMore = !reset,
                error = null,
            )
            val range = _uiState.value.dateRange
            when (val result = repository.getOrders(_uiState.value.statusFilter, targetPage, from = range.fromIso, to = range.toIso)) {
                is ApiResult.Success -> {
                    val merged = if (reset) result.data.content else _uiState.value.orders + result.data.content
                    _uiState.value = _uiState.value.copy(
                        orders = merged,
                        page = targetPage,
                        hasMore = targetPage < result.data.totalPages - 1,
                        isLoading = false,
                        isLoadingMore = false,
                    )
                }
                is ApiResult.Failure -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    error = if (_uiState.value.orders.isEmpty()) result.message else null,
                )
            }
        }
    }

    fun openCancelDialog(order: OrderResponse) {
        _uiState.value = _uiState.value.copy(cancelTarget = order, cancelReason = "", cancelError = null)
    }

    fun updateCancelReason(reason: String) {
        _uiState.value = _uiState.value.copy(cancelReason = reason)
    }

    fun dismissCancelDialog() {
        _uiState.value = _uiState.value.copy(cancelTarget = null, cancelError = null)
    }

    fun confirmCancel() {
        val order = _uiState.value.cancelTarget ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCancelling = true, cancelError = null)
            when (val result = repository.cancelOrder(order.id, _uiState.value.cancelReason.takeIf { it.isNotBlank() })) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(isCancelling = false, cancelTarget = null)
                    load(reset = true)
                }
                is ApiResult.Failure -> _uiState.value = _uiState.value.copy(isCancelling = false, cancelError = result.message)
            }
        }
    }
}
