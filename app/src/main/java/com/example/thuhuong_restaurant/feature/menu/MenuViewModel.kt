package com.example.thuhuong_restaurant.feature.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thuhuong_restaurant.core.common.ApiResult
import com.example.thuhuong_restaurant.core.model.ProductCategory
import com.example.thuhuong_restaurant.core.model.ProductResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MenuUiState(
    val products: List<ProductResponse> = emptyList(),
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val selectedCategory: ProductCategory? = null,
    val keyword: String = "",
    val page: Int = 0,
    val hasMore: Boolean = true,
)

@HiltViewModel
class MenuViewModel @Inject constructor(
    private val repository: MenuRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MenuUiState())
    val uiState: StateFlow<MenuUiState> = _uiState.asStateFlow()

    init {
        loadProducts(reset = true)
    }

    fun onCategorySelected(category: ProductCategory?) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
        loadProducts(reset = true)
    }

    fun onKeywordChange(keyword: String) {
        _uiState.value = _uiState.value.copy(keyword = keyword)
    }

    fun onSearchSubmit() {
        loadProducts(reset = true)
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || state.isLoading || !state.hasMore) return
        loadProducts(reset = false)
    }

    fun retry() = loadProducts(reset = true)

    private fun loadProducts(reset: Boolean) {
        val current = _uiState.value
        val page = if (reset) 0 else current.page + 1
        viewModelScope.launch {
            _uiState.value = current.copy(isLoading = reset, isLoadingMore = !reset, error = null)
            when (
                val result = repository.getProducts(
                    category = current.selectedCategory?.name,
                    keyword = current.keyword,
                    page = page,
                )
            ) {
                is ApiResult.Success -> {
                    val pageData = result.data
                    _uiState.value = _uiState.value.copy(
                        products = if (reset) pageData.content else _uiState.value.products + pageData.content,
                        isLoading = false,
                        isLoadingMore = false,
                        page = page,
                        hasMore = page < pageData.totalPages - 1,
                    )
                }
                is ApiResult.Failure -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        error = result.message,
                    )
                }
            }
        }
    }
}
