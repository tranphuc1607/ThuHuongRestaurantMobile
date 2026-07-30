package com.example.thuhuong_restaurant.feature.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.example.thuhuong_restaurant.core.common.dismissKeyboardOnTap
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.thuhuong_restaurant.core.model.ProductCategory
import com.example.thuhuong_restaurant.core.model.ProductResponse
import com.example.thuhuong_restaurant.core.model.label
import com.example.thuhuong_restaurant.core.ui.components.CategoryPill
import com.example.thuhuong_restaurant.core.ui.components.GradientHero
import com.example.thuhuong_restaurant.core.ui.components.ProductCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(
    onProductClick: (String) -> Unit,
    viewModel: MenuViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .dismissKeyboardOnTap(),
        ) {
            GradientHero(title = "Thực Đơn", subtitle = "Hơn 50 món đặc sắc từ ba miền Việt Nam")

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background),
            ) {
                CategoryFilterRow(
                    selected = state.selectedCategory,
                    onSelect = viewModel::onCategorySelected,
                )
                OutlinedTextField(
                    value = state.keyword,
                    onValueChange = viewModel::onKeywordChange,
                    placeholder = { Text("Tìm món ăn...") },
                    leadingIcon = {
                        IconButton(onClick = {
                            viewModel.onSearchSubmit()
                            keyboardController?.hide()
                        }) {
                            Icon(Icons.Filled.Search, contentDescription = "Tìm kiếm")
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        viewModel.onSearchSubmit()
                        keyboardController?.hide()
                    }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
                LaunchedEffect(state.keyword) {
                    kotlinx.coroutines.delay(400)
                    viewModel.onSearchSubmit()
                }
            }

            when {
                state.isLoading -> LoadingBox()
                state.error != null && state.products.isEmpty() -> ErrorBox(state.error!!, onRetry = viewModel::retry)
                else -> ProductGrid(
                    products = state.products,
                    isLoadingMore = state.isLoadingMore,
                    onLoadMore = viewModel::loadMore,
                    onProductClick = onProductClick,
                )
            }
        }
    }
}

@Composable
private fun CategoryFilterRow(
    selected: ProductCategory?,
    onSelect: (ProductCategory?) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            CategoryPill(label = "Tất cả", selected = selected == null, onClick = { onSelect(null) })
        }
        items(ProductCategory.entries.toList()) { category ->
            CategoryPill(
                label = category.label(),
                selected = selected == category,
                onClick = { onSelect(category) },
            )
        }
    }
}

@Composable
private fun ProductGrid(
    products: List<ProductResponse>,
    isLoadingMore: Boolean,
    onLoadMore: () -> Unit,
    onProductClick: (String) -> Unit,
) {
    if (products.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Không tìm thấy món ăn nào", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        itemsIndexed(products) { index, product ->
            if (index >= products.size - 4) onLoadMore()
            ProductCard(product = product, onClick = { onProductClick(product.id) })
        }
        if (isLoadingMore) {
            item(span = { GridItemSpan(2) }) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun LoadingBox() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun ErrorBox(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(message, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = onRetry) { Text("Thử lại", color = MaterialTheme.colorScheme.primary) }
        }
    }
}
