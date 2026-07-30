package com.example.thuhuong_restaurant.feature.employee.receiptscan

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.thuhuong_restaurant.core.common.createReceiptImageUri
import com.example.thuhuong_restaurant.core.common.formatPrice
import com.example.thuhuong_restaurant.core.model.MatchCandidate
import com.example.thuhuong_restaurant.core.model.ScanConfidence
import com.example.thuhuong_restaurant.core.model.TableResponse
import com.example.thuhuong_restaurant.core.model.TableStatus
import com.example.thuhuong_restaurant.core.ui.components.CategoryPill
import com.example.thuhuong_restaurant.core.ui.theme.ThSuccess
import com.example.thuhuong_restaurant.core.ui.theme.ThWarning
import com.example.thuhuong_restaurant.feature.employee.EmployeeTabRow
import com.example.thuhuong_restaurant.feature.employee.order.ProductPickerContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptScanScreen(
    navController: NavHostController,
    onOrderReady: (String) -> Unit,
    viewModel: ReceiptScanViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // submitWarning != null means some items failed to add — hold on this screen so the employee sees
    // which ones before navigating away, instead of silently landing on a short order.
    LaunchedEffect(state.createdOrderId, state.submitWarning) {
        val orderId = state.createdOrderId
        if (orderId != null && state.submitWarning == null) {
            onOrderReady(orderId)
            viewModel.onNavigationConsumed()
        }
    }

    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) pendingCameraUri?.let { viewModel.onImagesCaptured(listOf(it)) }
    }
    // Multi-select — a handwritten receipt often spans several sheets (one photo per sheet).
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(10)) { uris ->
        viewModel.onImagesCaptured(uris)
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Quét hóa đơn") },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                )
                EmployeeTabRow(navController)
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (state.step) {
                ScanStep.CAPTURE -> CaptureContent(
                    imageUris = state.imageUris,
                    error = state.analyzeError,
                    onCapture = {
                        val uri = context.createReceiptImageUri()
                        pendingCameraUri = uri
                        cameraLauncher.launch(uri)
                    },
                    onPickGallery = {
                        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onRemoveImage = viewModel::removeImage,
                    onAnalyze = viewModel::analyze,
                )
                ScanStep.ANALYZING -> AnalyzingContent()
                ScanStep.REVIEW -> ReviewContent(state = state, viewModel = viewModel)
            }
        }
    }

    if (state.pickerTarget != null) {
        ModalBottomSheet(onDismissRequest = viewModel::closeProductPicker) {
            ProductPickerContent(
                picker = state.picker,
                onCategorySelected = viewModel::onPickerCategorySelected,
                onKeywordChange = viewModel::onPickerKeywordChange,
                onProductSelected = viewModel::selectProductForTarget,
            )
        }
    }
}

@Composable
private fun CaptureContent(
    imageUris: List<Uri>,
    error: String?,
    onCapture: () -> Unit,
    onPickGallery: () -> Unit,
    onRemoveImage: (Uri) -> Unit,
    onAnalyze: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (imageUris.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.ReceiptLong,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Chụp hoặc chọn ảnh hóa đơn viết tay", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Hóa đơn nhiều tờ? Chụp lần lượt từng tờ, mỗi tờ 1 ảnh",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            Text(
                if (imageUris.size == 1) "1 ảnh hóa đơn" else "${imageUris.size} ảnh hóa đơn (nhiều tờ)",
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(10.dp))
            LazyRow(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                itemsIndexed(imageUris, key = { _, uri -> uri.toString() }) { index, uri ->
                    Box {
                        AsyncImage(
                            model = uri,
                            contentDescription = "Trang ${index + 1}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(150.dp)
                                .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.small),
                        )
                        Text(
                            "${index + 1}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .background(Color.Black.copy(alpha = 0.55f))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                        IconButton(
                            onClick = { onRemoveImage(uri) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(2.dp)
                                .size(26.dp)
                                .background(MaterialTheme.colorScheme.error, CircleShape),
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Xóa trang ${index + 1}",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
        }

        if (error != null) {
            Spacer(Modifier.height(12.dp))
            Text(error, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onCapture, shape = MaterialTheme.shapes.small, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (imageUris.isEmpty()) "Chụp ảnh" else "Chụp thêm")
            }
            OutlinedButton(onClick = onPickGallery, shape = MaterialTheme.shapes.small, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Thư viện")
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onAnalyze,
            enabled = imageUris.isNotEmpty(),
            shape = MaterialTheme.shapes.small,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (imageUris.size > 1) "Phân tích ${imageUris.size} ảnh" else "Phân tích hóa đơn")
        }
    }
}

@Composable
private fun AnalyzingContent() {
    val transition = rememberInfiniteTransition(label = "analyzing")
    val alpha by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(800), repeatMode = RepeatMode.Reverse),
        label = "alpha",
    )
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Text(
            "Đang phân tích hóa đơn...",
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = alpha),
        )
    }
}

@Composable
private fun ReviewContent(state: ReceiptScanUiState, viewModel: ReceiptScanViewModel) {
    val high = state.items.filter { it.confidence == ScanConfidence.HIGH }
    val low = state.items.filter { it.confidence == ScanConfidence.LOW }
    val none = state.items.filter { it.confidence == ScanConfidence.NONE }

    Column(modifier = Modifier.fillMaxSize()) {
        if (state.items.isEmpty() && state.beerRounds.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Không nhận diện được món nào trong ảnh", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (state.scanWarnings.isNotEmpty()) {
                    item {
                        Card(
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = ThWarning.copy(alpha = 0.15f)),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                state.scanWarnings.forEach {
                                    Text("⚠️ $it", color = ThWarning, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
                // Hàng "Bia" nằm riêng ở đầu tờ hóa đơn nên cũng để riêng ở đầu màn hình duyệt
                if (state.beerRounds.isNotEmpty() || state.canAddBeer) {
                    item {
                        BeerRoundsSection(
                            state = state,
                            onIncrement = viewModel::incrementBeer,
                            onDecrement = viewModel::decrementBeer,
                            onRemove = viewModel::removeBeerRound,
                            onAdd = viewModel::addBeerRound,
                        )
                    }
                }
                if (low.isNotEmpty()) {
                    item { SectionHeader("Cần xác nhận", ThWarning) }
                    items(low, key = { it.localId }) { item ->
                        ReviewItemRow(
                            item = item,
                            accentColor = ThWarning,
                            onIncrement = { viewModel.incrementQty(item.localId) },
                            onDecrement = { viewModel.decrementQty(item.localId) },
                            onRemove = { viewModel.removeItem(item.localId) },
                            onPickCandidate = { viewModel.applyCandidate(item.localId, it) },
                        ) {
                            IconButton(onClick = { viewModel.confirmSuggestion(item.localId) }) {
                                Icon(Icons.Filled.Check, contentDescription = "Xác nhận đúng", tint = ThSuccess)
                            }
                            IconButton(onClick = { viewModel.rejectSuggestion(item.localId) }) {
                                Icon(Icons.Filled.Close, contentDescription = "Từ chối", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
                if (none.isNotEmpty()) {
                    item { SectionHeader("Không nhận dạng được", MaterialTheme.colorScheme.error) }
                    items(none, key = { it.localId }) { item ->
                        ReviewItemRow(
                            item = item,
                            accentColor = MaterialTheme.colorScheme.error,
                            onIncrement = { viewModel.incrementQty(item.localId) },
                            onDecrement = { viewModel.decrementQty(item.localId) },
                            onRemove = { viewModel.removeItem(item.localId) },
                            onPickCandidate = { viewModel.applyCandidate(item.localId, it) },
                        ) {
                            TextButton(onClick = { viewModel.openProductPicker(PickerTarget.Reassign(item.localId)) }) {
                                Text("Chọn món", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
                if (high.isNotEmpty()) {
                    item { SectionHeader("Đã xác nhận (${high.size})", ThSuccess) }
                    items(high, key = { it.localId }) { item ->
                        ReviewItemRow(
                            item = item,
                            accentColor = MaterialTheme.colorScheme.outline,
                            onIncrement = { viewModel.incrementQty(item.localId) },
                            onDecrement = { viewModel.decrementQty(item.localId) },
                            onRemove = { viewModel.removeItem(item.localId) },
                        )
                    }
                }
                item {
                    TextButton(onClick = { viewModel.openProductPicker(PickerTarget.New) }) {
                        Text("+ Thêm món thủ công", color = MaterialTheme.colorScheme.primary)
                    }
                }
                if (state.tables.isNotEmpty()) {
                    item {
                        TableAssignmentRow(
                            tables = state.tables,
                            selectedTableId = state.selectedTableId,
                            detectedLabel = state.detectedTableLabel,
                            onSelect = viewModel::selectTable,
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Column(modifier = Modifier.padding(16.dp)) {
            if (state.createdOrderId != null && state.submitWarning != null) {
                Text(state.submitWarning, color = ThWarning, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = viewModel::onViewOrder,
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.buttonColors(containerColor = ThWarning),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Xem đơn hàng")
                }
            } else {
                Text(
                    buildString {
                        append("${state.highConfidenceCount} món đã xác nhận")
                        if (state.beerRounds.isNotEmpty()) {
                            append(" + ${state.beerRounds.size} đợt bia (${state.totalCups} cốc)")
                        }
                        append(" sẽ được thêm vào đơn")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (state.submitError != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(state.submitError, color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = viewModel::retakePhoto,
                        enabled = !state.isSubmitting,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("← Chụp lại")
                    }
                    Button(
                        onClick = viewModel::confirmAndSubmit,
                        enabled = (state.highConfidenceCount > 0 || state.beerRounds.isNotEmpty()) && !state.isSubmitting,
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(if (state.isSubmitting) "Đang tạo đơn..." else "Thêm vào đơn")
                    }
                }
            }
        }
    }
}

/**
 * Hàng "Bia" trên tờ hóa đơn: mỗi ô đã ghi là một lần khách gọi thêm. Hiển thị đúng từng ô để
 * nhân viên đối chiếu với tờ giấy, và khi tạo đơn mỗi ô sẽ thành một đợt riêng.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BeerRoundsSection(
    state: ReceiptScanUiState,
    onIncrement: (String) -> Unit,
    onDecrement: (String) -> Unit,
    onRemove: (String) -> Unit,
    onAdd: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "🍺 " + (state.beerProductName ?: "Bia cốc") +
                    if (state.beerRounds.isNotEmpty()) " · ${state.beerRounds.size} đợt" else "",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            if (state.totalCups > 0) {
                Text(
                    "${state.totalCups} cốc",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    formatPrice(state.beerTotal),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        if (state.beerRounds.isEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(
                "Hàng Bia trên hóa đơn để trống",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        } else {
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.beerRounds.forEach { round ->
                    Card(
                        shape = MaterialTheme.shapes.small,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    ) {
                        Row(
                            modifier = Modifier.padding(2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(
                                onClick = { onDecrement(round.localId) },
                                enabled = round.quantity > 1,
                                modifier = Modifier.size(34.dp),
                            ) {
                                Icon(Icons.Filled.Remove, contentDescription = "Bớt một cốc", modifier = Modifier.size(16.dp))
                            }
                            Text(
                                "${round.quantity}",
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.widthIn(min = 24.dp),
                            )
                            IconButton(onClick = { onIncrement(round.localId) }, modifier = Modifier.size(34.dp)) {
                                Icon(Icons.Filled.Add, contentDescription = "Thêm một cốc", modifier = Modifier.size(16.dp))
                            }
                            IconButton(onClick = { onRemove(round.localId) }, modifier = Modifier.size(34.dp)) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "Bỏ cả đợt",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }
            }
        }

        if (state.canAddBeer) {
            TextButton(onClick = onAdd) {
                Text("+ Thêm đợt bia", color = MaterialTheme.colorScheme.primary)
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
private fun SectionHeader(text: String, color: Color) {
    Text(text, fontWeight = FontWeight.Bold, color = color)
}

@Composable
private fun ReviewItemRow(
    item: ReviewItem,
    accentColor: Color,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onRemove: () -> Unit,
    onPickCandidate: ((MatchCandidate) -> Unit)? = null,
    trailingActions: (@Composable () -> Unit)? = null,
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
        border = BorderStroke(1.dp, accentColor),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        // Số thứ tự giúp nhân viên dò ngược lên tờ giấy
                        item.rowNumber?.let { "$it. ${item.productName}" } ?: item.productName,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    if (!item.rawText.isNullOrBlank() && item.rawText != item.productName) {
                        Text(
                            "\"${item.rawText}\"",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    if (item.unitPrice != null) {
                        Text(
                            formatPrice(item.unitPrice) + (item.lineTotal?.let { " → ${formatPrice(it)}" } ?: ""),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                IconButton(onClick = onRemove) {
                    Icon(Icons.Filled.Delete, contentDescription = "Xóa", tint = MaterialTheme.colorScheme.error)
                }
            }

            item.warnings.forEach {
                Text("⚠️ $it", color = ThWarning, style = MaterialTheme.typography.bodySmall)
            }

            if (onPickCandidate != null && item.candidates.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text("Gợi ý:", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(item.candidates, key = { it.productId }) { c ->
                        CategoryPill(
                            label = c.price?.let { "${c.productName} · ${formatPrice(it)}" } ?: c.productName,
                            selected = false,
                            onClick = { onPickCandidate(c) },
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onDecrement) { Icon(Icons.Filled.Remove, contentDescription = "Giảm") }
                Text("${item.quantity}", fontWeight = FontWeight.SemiBold)
                IconButton(onClick = onIncrement) { Icon(Icons.Filled.Add, contentDescription = "Tăng") }
                Spacer(Modifier.weight(1f))
                trailingActions?.invoke()
            }
        }
    }
}

@Composable
private fun TableAssignmentRow(
    tables: List<TableResponse>,
    selectedTableId: String?,
    detectedLabel: String?,
    onSelect: (String) -> Unit,
) {
    Column {
        Text(
            "Bàn (tùy chọn — để trống nếu là đơn quầy)",
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (!detectedLabel.isNullOrBlank()) {
            Text(
                "Hóa đơn ghi bàn số $detectedLabel",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Spacer(Modifier.height(8.dp))
        val available = tables.filter { it.status == TableStatus.AVAILABLE }
        if (available.isEmpty()) {
            Text("Không có bàn trống", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(available, key = { it.id }) { table ->
                    CategoryPill(label = table.name, selected = table.id == selectedTableId, onClick = { onSelect(table.id) })
                }
            }
        }
    }
}
