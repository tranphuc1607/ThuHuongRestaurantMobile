package com.example.thuhuong_restaurant.feature.employee.booking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thuhuong_restaurant.core.common.ApiResult
import com.example.thuhuong_restaurant.core.common.DateRangeState
import com.example.thuhuong_restaurant.core.model.BookingResponse
import com.example.thuhuong_restaurant.core.model.BookingStatus
import com.example.thuhuong_restaurant.core.model.TableResponse
import com.example.thuhuong_restaurant.core.model.allowsFutureDateFilter
import com.example.thuhuong_restaurant.feature.employee.table.TableRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BookingListUiState(
    val statusFilter: BookingStatus? = BookingStatus.CONFIRMED,
    val dateRange: DateRangeState = DateRangeState(),
    val bookings: List<BookingResponse> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val page: Int = 0,
    val hasMore: Boolean = false,
    val isLoadingMore: Boolean = false,
    val checkoutTarget: BookingResponse? = null,
    val tables: List<TableResponse> = emptyList(),
    val isLoadingTables: Boolean = false,
    val selectedTableId: String? = null,
    val isCheckingOut: Boolean = false,
    val checkoutError: String? = null,
    val navigateToOrderId: String? = null,
    val completeTarget: BookingResponse? = null,
    val isCompleting: Boolean = false,
    val completeError: String? = null,
)

@HiltViewModel
class BookingListViewModel @Inject constructor(
    private val bookingRepository: BookingRepository,
    private val tableRepository: TableRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookingListUiState())
    val uiState: StateFlow<BookingListUiState> = _uiState.asStateFlow()

    init {
        load(reset = true)
        pollSilently()
    }

    private fun pollSilently() {
        viewModelScope.launch {
            while (true) {
                delay(10_000)
                // Only silently refresh while on the first page — avoids clobbering an accumulated "load more" list.
                if (_uiState.value.page == 0 && _uiState.value.checkoutTarget == null && _uiState.value.completeTarget == null) {
                    load(reset = true, showLoading = false)
                }
            }
        }
    }

    fun selectFilter(status: BookingStatus?) {
        if (status == _uiState.value.statusFilter) return
        val range = _uiState.value.dateRange
        val clampedRange = if (!status.allowsFutureDateFilter() && range.offset > 0) range.copy(offset = 0) else range
        _uiState.value = _uiState.value.copy(statusFilter = status, dateRange = clampedRange)
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

    private fun load(reset: Boolean, showLoading: Boolean = true) {
        viewModelScope.launch {
            val targetPage = if (reset) 0 else _uiState.value.page + 1
            _uiState.value = _uiState.value.copy(
                isLoading = showLoading && reset && _uiState.value.bookings.isEmpty(),
                isLoadingMore = !reset,
                error = null,
            )
            val filter = _uiState.value.statusFilter
            val range = _uiState.value.dateRange
            when (
                val result = bookingRepository.getBookings(
                    filter?.name, excludePending = filter == null, page = targetPage, from = range.fromIso, to = range.toIso,
                )
            ) {
                is ApiResult.Success -> {
                    val merged = if (reset) result.data.content else _uiState.value.bookings + result.data.content
                    _uiState.value = _uiState.value.copy(
                        bookings = merged,
                        page = targetPage,
                        hasMore = targetPage < result.data.totalPages - 1,
                        isLoading = false,
                        isLoadingMore = false,
                    )
                }
                is ApiResult.Failure -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    error = if (_uiState.value.bookings.isEmpty()) result.message else null,
                )
            }
        }
    }

    fun openCheckoutDialog(booking: BookingResponse) {
        _uiState.value = _uiState.value.copy(
            checkoutTarget = booking,
            selectedTableId = null,
            checkoutError = null,
            isLoadingTables = true,
        )
        viewModelScope.launch {
            when (val result = tableRepository.getTables()) {
                is ApiResult.Success -> _uiState.value = _uiState.value.copy(tables = result.data, isLoadingTables = false)
                is ApiResult.Failure -> _uiState.value = _uiState.value.copy(isLoadingTables = false, checkoutError = result.message)
            }
        }
    }

    fun selectTable(tableId: String) {
        _uiState.value = _uiState.value.copy(selectedTableId = tableId)
    }

    fun dismissCheckoutDialog() {
        _uiState.value = _uiState.value.copy(checkoutTarget = null, selectedTableId = null, checkoutError = null)
    }

    fun confirmCheckout() {
        val booking = _uiState.value.checkoutTarget ?: return
        val tableId = _uiState.value.selectedTableId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCheckingOut = true, checkoutError = null)
            when (val result = bookingRepository.checkoutToOrder(booking.id, tableId)) {
                is ApiResult.Success -> _uiState.value = _uiState.value.copy(
                    isCheckingOut = false,
                    checkoutTarget = null,
                    navigateToOrderId = result.data.id,
                )
                is ApiResult.Failure -> _uiState.value = _uiState.value.copy(isCheckingOut = false, checkoutError = result.message)
            }
        }
    }

    fun onNavigationConsumed() {
        _uiState.value = _uiState.value.copy(navigateToOrderId = null)
        load(reset = true)
    }

    fun openCompleteDialog(booking: BookingResponse) {
        _uiState.value = _uiState.value.copy(completeTarget = booking, completeError = null)
    }

    fun dismissCompleteDialog() {
        _uiState.value = _uiState.value.copy(completeTarget = null, completeError = null)
    }

    fun confirmComplete() {
        val booking = _uiState.value.completeTarget ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCompleting = true, completeError = null)
            when (val result = bookingRepository.completeBooking(booking.id)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(isCompleting = false, completeTarget = null)
                    load(reset = true)
                }
                is ApiResult.Failure -> _uiState.value = _uiState.value.copy(isCompleting = false, completeError = result.message)
            }
        }
    }
}
