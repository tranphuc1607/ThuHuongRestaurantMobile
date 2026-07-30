package com.example.thuhuong_restaurant.feature.menu

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thuhuong_restaurant.core.common.ApiResult
import com.example.thuhuong_restaurant.core.model.ProductResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProductDetailUiState(
    val product: ProductResponse? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    private val repository: MenuRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val productId: String = checkNotNull(savedStateHandle["productId"])

    private val _uiState = MutableStateFlow(ProductDetailUiState())
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            when (val result = repository.getProduct(productId)) {
                is ApiResult.Success ->
                    _uiState.value = ProductDetailUiState(product = result.data, isLoading = false)
                is ApiResult.Failure ->
                    _uiState.value = ProductDetailUiState(isLoading = false, error = result.message)
            }
        }
    }
}
