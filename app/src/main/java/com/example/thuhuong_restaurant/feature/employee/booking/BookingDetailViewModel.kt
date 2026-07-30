package com.example.thuhuong_restaurant.feature.employee.booking

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thuhuong_restaurant.core.common.ApiResult
import com.example.thuhuong_restaurant.core.model.BookingResponse
import com.example.thuhuong_restaurant.core.model.TableResponse
import com.example.thuhuong_restaurant.feature.employee.table.TableRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BookingDetailUiState(
    val booking: BookingResponse? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val showCheckoutDialog: Boolean = false,
    val tables: List<TableResponse> = emptyList(),
    val isLoadingTables: Boolean = false,
    val selectedTableId: String? = null,
    val isCheckingOut: Boolean = false,
    val checkoutError: String? = null,
    val navigateToOrderId: String? = null,
    val showCompleteDialog: Boolean = false,
    val isCompleting: Boolean = false,
    val completeError: String? = null,
)

@HiltViewModel
class BookingDetailViewModel @Inject constructor(
    private val bookingRepository: BookingRepository,
    private val tableRepository: TableRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val bookingCode: String = checkNotNull(savedStateHandle["bookingCode"])

    private val _uiState = MutableStateFlow(BookingDetailUiState())
    val uiState: StateFlow<BookingDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = bookingRepository.getBookingByCode(bookingCode)) {
                is ApiResult.Success -> _uiState.value = _uiState.value.copy(booking = result.data, isLoading = false)
                is ApiResult.Failure -> _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
            }
        }
    }

    fun openCheckoutDialog() {
        _uiState.value = _uiState.value.copy(
            showCheckoutDialog = true,
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
        _uiState.value = _uiState.value.copy(showCheckoutDialog = false, selectedTableId = null, checkoutError = null)
    }

    fun confirmCheckout() {
        val booking = _uiState.value.booking ?: return
        val tableId = _uiState.value.selectedTableId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCheckingOut = true, checkoutError = null)
            when (val result = bookingRepository.checkoutToOrder(booking.id, tableId)) {
                is ApiResult.Success -> _uiState.value = _uiState.value.copy(
                    isCheckingOut = false,
                    showCheckoutDialog = false,
                    navigateToOrderId = result.data.id,
                )
                is ApiResult.Failure -> _uiState.value = _uiState.value.copy(isCheckingOut = false, checkoutError = result.message)
            }
        }
    }

    fun onNavigationConsumed() {
        _uiState.value = _uiState.value.copy(navigateToOrderId = null)
    }

    fun openCompleteDialog() {
        _uiState.value = _uiState.value.copy(showCompleteDialog = true, completeError = null)
    }

    fun dismissCompleteDialog() {
        _uiState.value = _uiState.value.copy(showCompleteDialog = false, completeError = null)
    }

    fun confirmComplete() {
        val booking = _uiState.value.booking ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCompleting = true, completeError = null)
            when (val result = bookingRepository.completeBooking(booking.id)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(isCompleting = false, showCompleteDialog = false, booking = result.data)
                }
                is ApiResult.Failure -> _uiState.value = _uiState.value.copy(isCompleting = false, completeError = result.message)
            }
        }
    }
}
