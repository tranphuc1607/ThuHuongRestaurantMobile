package com.example.thuhuong_restaurant.feature.employee.booking

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.thuhuong_restaurant.core.model.TableResponse
import com.example.thuhuong_restaurant.core.model.TableStatus
import com.example.thuhuong_restaurant.core.ui.theme.ThSuccess

/** Shared table-picker dialog used by both the bookings list and booking detail screens for "checkout-to-order". */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableSelectionDialog(
    tables: List<TableResponse>,
    isLoadingTables: Boolean,
    selectedTableId: String?,
    onSelectTable: (String) -> Unit,
    isSubmitting: Boolean,
    error: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val selectedIsOccupied = tables.find { it.id == selectedTableId }?.status == TableStatus.OCCUPIED

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Chọn bàn") },
        text = {
            Column {
                if (isLoadingTables) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.height(260.dp),
                    ) {
                        items(tables, key = { it.id }) { table ->
                            val occupied = table.status == TableStatus.OCCUPIED
                            val color = if (occupied) MaterialTheme.colorScheme.error else ThSuccess
                            val selected = table.id == selectedTableId
                            Card(
                                onClick = { onSelectTable(table.id) },
                                shape = MaterialTheme.shapes.small,
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selected) color.copy(alpha = 0.25f) else color.copy(alpha = 0.12f),
                                ),
                                border = BorderStroke(if (selected) 2.dp else 1.dp, color),
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        table.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = MaterialTheme.typography.bodySmall.fontSize,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            }
                        }
                    }
                    if (selectedIsOccupied) {
                        Text(
                            "Bàn này đang có khách — vui lòng chọn bàn khác",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
                if (error != null) {
                    Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = selectedTableId != null && !selectedIsOccupied && !isSubmitting && !isLoadingTables,
            ) {
                Text("Xác nhận", color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        },
    )
}

