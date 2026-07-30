package com.example.thuhuong_restaurant.feature.employee.table

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thuhuong_restaurant.core.common.ApiResult
import com.example.thuhuong_restaurant.core.model.TableResponse
import com.example.thuhuong_restaurant.core.model.needsRefund
import com.example.thuhuong_restaurant.core.model.netRefundOf
import com.example.thuhuong_restaurant.feature.employee.order.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TableMapUiState(
    val tables: List<TableResponse> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val selectedTableForCreate: TableResponse? = null,
    val isCreatingOrder: Boolean = false,
    val actionError: String? = null,
    val navigateToOrderId: String? = null,
    /** tableId -> amount owed back to the customer (booking deposit exceeds current order total). */
    val refundByTableId: Map<String, Double> = emptyMap(),
)

@HiltViewModel
class TableMapViewModel @Inject constructor(
    private val repository: TableRepository,
    private val orderRepository: OrderRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TableMapUiState())
    val uiState: StateFlow<TableMapUiState> = _uiState.asStateFlow()

    init {
        pollTables()
    }

    private fun pollTables() {
        viewModelScope.launch {
            while (true) {
                refreshTables(showLoading = _uiState.value.tables.isEmpty())
                delay(3000)
            }
        }
    }

    private suspend fun refreshTables(showLoading: Boolean) {
        if (showLoading) _uiState.value = _uiState.value.copy(isLoading = true)
        when (val result = repository.getTables()) {
            is ApiResult.Success -> _uiState.value = _uiState.value.copy(
                tables = result.data.sortedBy { it.tableNumber },
                isLoading = false,
                error = null,
            )
            is ApiResult.Failure -> _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = if (_uiState.value.tables.isEmpty()) result.message else null,
            )
        }
        refreshRefundMap()
    }

    private suspend fun refreshRefundMap() {
        when (val result = orderRepository.getOrders(status = "OPEN", page = 0, size = 100)) {
            is ApiResult.Success -> {
                val map = result.data.content
                    .filter { it.needsRefund() && it.tableId != null }
                    .associate { it.tableId!! to netRefundOf(it.prepaidAmount, it.totalAmount) }
                _uiState.value = _uiState.value.copy(refundByTableId = map)
            }
            is ApiResult.Failure -> Unit
        }
    }

    fun retry() {
        viewModelScope.launch { refreshTables(showLoading = true) }
    }

    fun onTableTap(table: TableResponse) {
        if (table.status == com.example.thuhuong_restaurant.core.model.TableStatus.OCCUPIED) {
            openOccupiedTable(table)
        } else {
            _uiState.value = _uiState.value.copy(selectedTableForCreate = table, actionError = null)
        }
    }

    fun dismissCreateDialog() {
        _uiState.value = _uiState.value.copy(selectedTableForCreate = null, actionError = null)
    }

    private fun openOccupiedTable(table: TableResponse) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreatingOrder = true, actionError = null)
            when (val result = repository.getOrderByTableQrToken(table.qrToken)) {
                is ApiResult.Success -> _uiState.value = _uiState.value.copy(
                    isCreatingOrder = false,
                    navigateToOrderId = result.data.id,
                )
                is ApiResult.Failure -> _uiState.value = _uiState.value.copy(
                    isCreatingOrder = false,
                    actionError = result.message,
                )
            }
        }
    }

    fun confirmCreateOrder(note: String) {
        val table = _uiState.value.selectedTableForCreate ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreatingOrder = true, actionError = null)
            when (val result = repository.createOrder(table.id, note.takeIf { it.isNotBlank() })) {
                is ApiResult.Success -> _uiState.value = _uiState.value.copy(
                    isCreatingOrder = false,
                    selectedTableForCreate = null,
                    navigateToOrderId = result.data.id,
                )
                is ApiResult.Failure -> _uiState.value = _uiState.value.copy(
                    isCreatingOrder = false,
                    actionError = result.message,
                )
            }
        }
    }

    fun onNavigationConsumed() {
        _uiState.value = _uiState.value.copy(navigateToOrderId = null)
    }
}
