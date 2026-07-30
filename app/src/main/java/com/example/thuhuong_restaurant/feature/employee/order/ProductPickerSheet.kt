package com.example.thuhuong_restaurant.feature.employee.order

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.thuhuong_restaurant.core.common.formatPrice
import com.example.thuhuong_restaurant.core.model.ProductCategory
import com.example.thuhuong_restaurant.core.model.ProductResponse
import com.example.thuhuong_restaurant.core.model.label
import com.example.thuhuong_restaurant.core.ui.components.CategoryPill

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductPickerContent(
    picker: ProductPickerState,
    onCategorySelected: (ProductCategory?) -> Unit,
    onKeywordChange: (String) -> Unit,
    onProductSelected: (ProductResponse) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(560.dp),
    ) {
        Text(
            "Thêm món",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        OutlinedTextField(
            value = picker.keyword,
            onValueChange = onKeywordChange,
            placeholder = { Text("Tìm món ăn...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            shape = MaterialTheme.shapes.small,
            colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = MaterialTheme.colorScheme.outline),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                CategoryPill(label = "Tất cả", selected = picker.selectedCategory == null, onClick = { onCategorySelected(null) })
            }
            items(ProductCategory.entries.toList()) { category ->
                CategoryPill(
                    label = category.label(),
                    selected = picker.selectedCategory == category,
                    onClick = { onCategorySelected(category) },
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (picker.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary,
                )
            } else if (picker.products.isEmpty()) {
                Text(
                    "Không tìm thấy món ăn",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center),
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(picker.products, key = { it.id }) { product ->
                        PickerProductRow(product = product, onClick = { onProductSelected(product) })
                    }
                }
            }
        }
    }
}

@Composable
private fun PickerProductRow(product: ProductResponse, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = product.imageUrl,
                contentDescription = product.name,
                modifier = Modifier
                    .size(48.dp)
                    .aspectRatio(1f)
                    .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp)),
            )
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(product.name, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground, maxLines = 1)
                Text(
                    formatPrice(product.price),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
