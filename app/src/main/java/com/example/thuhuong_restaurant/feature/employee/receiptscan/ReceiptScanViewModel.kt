package com.example.thuhuong_restaurant.feature.employee.receiptscan

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thuhuong_restaurant.core.common.ApiResult
import com.example.thuhuong_restaurant.core.common.uriToScaledJpegBase64
import com.example.thuhuong_restaurant.core.model.MatchCandidate
import com.example.thuhuong_restaurant.core.model.ProductCategory
import com.example.thuhuong_restaurant.core.model.ProductResponse
import com.example.thuhuong_restaurant.core.model.ScanConfidence
import com.example.thuhuong_restaurant.core.model.ScanFeedbackRequest
import com.example.thuhuong_restaurant.core.model.TableResponse
import com.example.thuhuong_restaurant.core.model.TableStatus
import com.example.thuhuong_restaurant.core.model.confidenceLevel
import com.example.thuhuong_restaurant.core.model.defaultSaleUnitFor
import com.example.thuhuong_restaurant.feature.employee.order.OrderRepository
import com.example.thuhuong_restaurant.feature.employee.order.ProductPickerState
import com.example.thuhuong_restaurant.feature.employee.table.TableRepository
import com.example.thuhuong_restaurant.feature.menu.MenuRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

enum class ScanStep { CAPTURE, ANALYZING, REVIEW }

/** Which slot a just-opened product picker is filling — a brand-new manual line, or reassigning an existing one. */
sealed class PickerTarget {
    data object New : PickerTarget()
    data class Reassign(val localId: String) : PickerTarget()
}

data class ReviewItem(
    val localId: String = UUID.randomUUID().toString(),
    val productId: String?,
    val productName: String,
    val quantity: Int,
    val saleUnit: String,
    val rawText: String?,
    val confidence: ScanConfidence,
    /** Cột STT trên hóa đơn — giúp nhân viên đối chiếu với tờ giấy. */
    val rowNumber: Int? = null,
    val unitPrice: Double? = null,
    val lineTotal: Double? = null,
    /** Gợi ý thay thế do server chấm điểm (khớp giá + khớp tên). */
    val candidates: List<MatchCandidate> = emptyList(),
    /** Cảnh báo từ server, ví dụ lệch phép tính. */
    val warnings: List<String> = emptyList(),
)

/**
 * Một ô trong hàng "Bia" của tờ hóa đơn = một lần khách gọi thêm bia. Giữ riêng từng ô thay vì
 * cộng gộp, để khi tạo đơn mỗi ô thành một đợt riêng đúng như trên giấy.
 */
data class BeerRound(
    val localId: String = UUID.randomUUID().toString(),
    val quantity: Int,
)

data class ReceiptScanUiState(
    val step: ScanStep = ScanStep.CAPTURE,
    val imageUris: List<Uri> = emptyList(),
    val analyzeError: String? = null,
    val items: List<ReviewItem> = emptyList(),
    val beerRounds: List<BeerRound> = emptyList(),
    val beerProductId: String? = null,
    val beerProductName: String? = null,
    val beerUnitPrice: Double? = null,
    val pickerTarget: PickerTarget? = null,
    val picker: ProductPickerState = ProductPickerState(),
    val tables: List<TableResponse> = emptyList(),
    val selectedTableId: String? = null,
    val isSubmitting: Boolean = false,
    val submitError: String? = null,
    val submitWarning: String? = null,
    val createdOrderId: String? = null,
    /** Cảnh báo cấp hóa đơn từ server, ví dụ tổng cộng lệch với tổng các dòng. */
    val scanWarnings: List<String> = emptyList(),
    /** Ô "Bàn số" đọc được từ hóa đơn, để hiển thị lý do bàn được chọn sẵn. */
    val detectedTableLabel: String? = null,
) {
    val highConfidenceCount: Int get() = items.count { it.confidence == ScanConfidence.HIGH }

    val totalCups: Int get() = beerRounds.sumOf { it.quantity }

    val beerTotal: Double get() = totalCups * (beerUnitPrice ?: 0.0)

    /** Có món bia trong thực đơn thì mới cho thêm đợt thủ công. */
    val canAddBeer: Boolean get() = beerProductId != null
}

@HiltViewModel
class ReceiptScanViewModel @Inject constructor(
    @param:ApplicationContext private val appContext: android.content.Context,
    private val receiptScanRepository: ReceiptScanRepository,
    private val orderRepository: OrderRepository,
    private val tableRepository: TableRepository,
    private val menuRepository: MenuRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReceiptScanUiState())
    val uiState: StateFlow<ReceiptScanUiState> = _uiState.asStateFlow()

    private var searchJob: kotlinx.coroutines.Job? = null

    /**
     * Ảnh được nén + mã hóa ngay khi vừa chụp/chọn, không đợi tới lúc bấm "Phân tích". Nhân viên
     * thường chụp tờ tiếp theo ngay sau đó, nên phần xử lý nặng (giải mã ảnh 12MP, xoay, tăng
     * tương phản, nén, base64) chạy lấp vào khoảng thời gian đó và biến mất khỏi thời gian chờ.
     */
    private val encodedPages = mutableMapOf<Uri, kotlinx.coroutines.Deferred<String?>>()

    init {
        loadTables()
    }

    private fun loadTables() {
        viewModelScope.launch {
            when (val result = tableRepository.getTables()) {
                is ApiResult.Success -> _uiState.value = _uiState.value.copy(tables = result.data)
                is ApiResult.Failure -> Unit
            }
        }
    }

    /** Appends — a receipt often spans multiple sheets (write to the end of one, continue on the next),
     *  so each capture/pick adds a page rather than replacing the previous one. */
    fun onImagesCaptured(uris: List<Uri>) {
        if (uris.isEmpty()) return
        uris.forEach { uri ->
            encodedPages.getOrPut(uri) {
                viewModelScope.async(kotlinx.coroutines.Dispatchers.Default) {
                    appContext.uriToScaledJpegBase64(uri)
                }
            }
        }
        _uiState.value = _uiState.value.copy(imageUris = _uiState.value.imageUris + uris, analyzeError = null)
    }

    fun removeImage(uri: Uri) {
        encodedPages.remove(uri)?.cancel()
        _uiState.value = _uiState.value.copy(imageUris = _uiState.value.imageUris.filterNot { it == uri })
    }

    fun analyze() {
        val uris = _uiState.value.imageUris
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(step = ScanStep.ANALYZING, analyzeError = null)
            val images = uriListToScaledBase64(uris)
            if (images.isEmpty()) {
                _uiState.value = _uiState.value.copy(step = ScanStep.CAPTURE, analyzeError = "Không đọc được ảnh, vui lòng thử lại")
                return@launch
            }
            // Trước đây trang lỗi bị bỏ âm thầm — nhân viên không biết đơn đang thiếu một tờ
            val droppedPages = uris.size - images.size
            if (droppedPages > 0) {
                _uiState.value = _uiState.value.copy(
                    step = ScanStep.CAPTURE,
                    analyzeError = "Có $droppedPages ảnh không đọc được. Vui lòng xóa và chụp lại trang đó.",
                )
                return@launch
            }
            when (val result = receiptScanRepository.scanReceipt(images, "image/jpeg")) {
                is ApiResult.Success -> {
                    val scan = result.data
                    val items = scan.items.map {
                        ReviewItem(
                            productId = it.productId,
                            productName = it.productName?.takeIf { n -> n.isNotBlank() } ?: (it.rawText ?: "Món chưa rõ"),
                            quantity = it.quantity.coerceAtLeast(1),
                            // saleUnit do server lấy từ DB của sản phẩm đã khớp, không phải AI đoán
                            saleUnit = it.saleUnit,
                            rawText = it.rawText,
                            confidence = it.confidenceLevel(),
                            rowNumber = it.rowNumber,
                            unitPrice = it.unitPrice,
                            lineTotal = it.lineTotal,
                            candidates = it.candidates,
                            warnings = it.warnings,
                        )
                    }
                    _uiState.value = _uiState.value.copy(
                        step = ScanStep.REVIEW,
                        items = items,
                        beerRounds = scan.beerRounds.map { qty -> BeerRound(quantity = qty.coerceAtLeast(1)) },
                        beerProductId = scan.beerProductId,
                        beerProductName = scan.beerProductName,
                        beerUnitPrice = scan.beerUnitPrice,
                        scanWarnings = scan.warnings,
                        detectedTableLabel = scan.tableLabel,
                        selectedTableId = matchTableId(scan.tableLabel) ?: _uiState.value.selectedTableId,
                    )
                }
                is ApiResult.Failure -> _uiState.value = _uiState.value.copy(step = ScanStep.CAPTURE, analyzeError = result.message)
            }
        }
    }

    /**
     * Lấy kết quả mã hóa đã chạy sẵn từ lúc chụp; nếu bấm "Phân tích" quá nhanh thì chờ nốt.
     * Bỏ qua (chứ không làm hỏng cả lô) trang nào không đọc được — số trang bị bỏ được báo cho
     * nhân viên ở [analyze].
     */
    private suspend fun uriListToScaledBase64(uris: List<Uri>): List<String> =
        uris.mapNotNull { uri ->
            val job = encodedPages.getOrPut(uri) {
                viewModelScope.async(kotlinx.coroutines.Dispatchers.Default) {
                    appContext.uriToScaledJpegBase64(uri)
                }
            }
            runCatching { job.await() }.getOrNull()
        }

    fun retakePhoto() {
        encodedPages.values.forEach { it.cancel() }
        encodedPages.clear()
        _uiState.value = ReceiptScanUiState(tables = _uiState.value.tables)
    }

    /**
     * Khớp ô "Bàn số" viết tay với bàn thật. Chỉ chọn sẵn bàn CÒN TRỐNG — bàn đang có khách sẽ bị
     * server từ chối khi tạo đơn, nên chọn sẵn cũng vô nghĩa.
     */
    private fun matchTableId(label: String?): String? {
        val digits = label?.filter { it.isDigit() }?.takeIf { it.isNotEmpty() } ?: return null
        val number = digits.toIntOrNull() ?: return null
        return _uiState.value.tables
            .firstOrNull { it.tableNumber == number && it.status == TableStatus.AVAILABLE }
            ?.id
    }

    // ── Item review/edit ───────────────────────────────────────────────────

    fun incrementQty(localId: String) = updateItem(localId) { it.copy(quantity = it.quantity + 1) }

    fun decrementQty(localId: String) = updateItem(localId) { it.copy(quantity = (it.quantity - 1).coerceAtLeast(1)) }

    fun removeItem(localId: String) {
        _uiState.value = _uiState.value.copy(items = _uiState.value.items.filterNot { it.localId == localId })
    }

    /** "✓" on a low-confidence row — accept the AI's best guess as-is. */
    fun confirmSuggestion(localId: String) = updateItem(localId) { it.copy(confidence = ScanConfidence.HIGH) }

    /** "✗" on a low-confidence row — AI's guess was wrong; drop back to unmatched so the employee can pick manually. */
    fun rejectSuggestion(localId: String) = updateItem(localId) { it.copy(productId = null, confidence = ScanConfidence.NONE) }

    /** Chọn nhanh một gợi ý do server chấm điểm — nhanh hơn mở picker và gõ tìm. */
    fun applyCandidate(localId: String, candidate: MatchCandidate) = updateItem(localId) {
        it.copy(
            productId = candidate.productId,
            productName = candidate.productName,
            confidence = ScanConfidence.HIGH,
            candidates = emptyList(),
            warnings = emptyList(),
        )
    }

    private fun updateItem(localId: String, transform: (ReviewItem) -> ReviewItem) {
        _uiState.value = _uiState.value.copy(
            items = _uiState.value.items.map { if (it.localId == localId) transform(it) else it },
        )
    }

    // ── Đợt bia (hàng "Bia" trên tờ hóa đơn) ───────────────────────────────

    fun incrementBeer(localId: String) = updateBeer(localId) { it.copy(quantity = it.quantity + 1) }

    fun decrementBeer(localId: String) = updateBeer(localId) { it.copy(quantity = (it.quantity - 1).coerceAtLeast(1)) }

    fun removeBeerRound(localId: String) {
        _uiState.value = _uiState.value.copy(beerRounds = _uiState.value.beerRounds.filterNot { it.localId == localId })
    }

    /** Thêm tay một ô bia mà AI đọc sót — nhân viên đối chiếu tờ giấy thấy thiếu thì bổ sung. */
    fun addBeerRound() {
        if (!_uiState.value.canAddBeer) return
        _uiState.value = _uiState.value.copy(beerRounds = _uiState.value.beerRounds + BeerRound(quantity = 1))
    }

    private fun updateBeer(localId: String, transform: (BeerRound) -> BeerRound) {
        _uiState.value = _uiState.value.copy(
            beerRounds = _uiState.value.beerRounds.map { if (it.localId == localId) transform(it) else it },
        )
    }

    // ── Product picker (manual add / reassign a "none" row) ────────────────

    fun openProductPicker(target: PickerTarget) {
        _uiState.value = _uiState.value.copy(pickerTarget = target, picker = ProductPickerState())
        loadPickerProducts()
    }

    fun closeProductPicker() {
        _uiState.value = _uiState.value.copy(pickerTarget = null)
    }

    fun onPickerCategorySelected(category: ProductCategory?) {
        _uiState.value = _uiState.value.copy(picker = _uiState.value.picker.copy(selectedCategory = category))
        loadPickerProducts()
    }

    fun onPickerKeywordChange(keyword: String) {
        _uiState.value = _uiState.value.copy(picker = _uiState.value.picker.copy(keyword = keyword))
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            kotlinx.coroutines.delay(400)
            loadPickerProducts()
        }
    }

    private fun loadPickerProducts() {
        viewModelScope.launch {
            val picker = _uiState.value.picker
            _uiState.value = _uiState.value.copy(picker = picker.copy(isLoading = true))
            when (
                val result = menuRepository.getProducts(
                    category = picker.selectedCategory?.name,
                    keyword = picker.keyword,
                    page = 0,
                    size = 100,
                )
            ) {
                is ApiResult.Success -> _uiState.value = _uiState.value.copy(
                    picker = _uiState.value.picker.copy(products = result.data.content, isLoading = false),
                )
                is ApiResult.Failure -> _uiState.value = _uiState.value.copy(picker = _uiState.value.picker.copy(isLoading = false))
            }
        }
    }

    fun selectProductForTarget(product: ProductResponse) {
        val target = _uiState.value.pickerTarget ?: return
        val saleUnit = defaultSaleUnitFor(product)
        when (target) {
            is PickerTarget.New -> {
                val newItem = ReviewItem(
                    productId = product.id,
                    productName = product.name,
                    quantity = 1,
                    saleUnit = saleUnit,
                    rawText = null,
                    confidence = ScanConfidence.HIGH,
                )
                _uiState.value = _uiState.value.copy(items = _uiState.value.items + newItem)
            }
            is PickerTarget.Reassign -> updateItem(target.localId) {
                it.copy(productId = product.id, productName = product.name, saleUnit = saleUnit, confidence = ScanConfidence.HIGH)
            }
        }
        _uiState.value = _uiState.value.copy(pickerTarget = null)
    }

    // ── Optional table assignment ───────────────────────────────────────────

    fun selectTable(tableId: String) {
        val current = _uiState.value.selectedTableId
        _uiState.value = _uiState.value.copy(selectedTableId = if (current == tableId) null else tableId)
    }

    // ── Submit ───────────────────────────────────────────────────────────────

    fun confirmAndSubmit() {
        val state = _uiState.value
        val validItems = state.items.filter { it.confidence == ScanConfidence.HIGH && it.productId != null }
        val beerProductId = state.beerProductId
        val beerRounds = if (beerProductId != null) state.beerRounds else emptyList()
        if (validItems.isEmpty() && beerRounds.isEmpty()) {
            _uiState.value = _uiState.value.copy(submitError = "Chưa có món nào được xác nhận (✓) để thêm vào đơn")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, submitError = null)

            val orderResult = orderRepository.createOrder(
                tableId = _uiState.value.selectedTableId,
                note = "Quét hóa đơn viết tay",
            )
            val order = when (orderResult) {
                is ApiResult.Success -> orderResult.data
                is ApiResult.Failure -> {
                    _uiState.value = _uiState.value.copy(isSubmitting = false, submitError = orderResult.message)
                    return@launch
                }
            }

            var successCount = 0
            var lastError: String? = null
            val failedNames = mutableListOf<String>()

            // Mỗi ô bia trên tờ giấy = một lần POST riêng ⇒ một đợt riêng trong đơn, đúng như
            // nhân viên đã ghi. Gộp lại thành một dòng là mất dấu từng lần khách gọi.
            beerRounds.forEachIndexed { index, round ->
                when (
                    val result = orderRepository.addItem(
                        orderId = order.id,
                        productId = beerProductId!!,
                        quantity = round.quantity,
                        saleUnit = "CUP",
                        skipInventoryDeduction = true,
                    )
                ) {
                    is ApiResult.Success -> successCount++
                    is ApiResult.Failure -> {
                        lastError = result.message
                        failedNames.add("bia đợt ${index + 1} (${round.quantity} cốc)")
                    }
                }
            }

            for (item in validItems) {
                when (
                    val result = orderRepository.addItem(
                        orderId = order.id,
                        productId = item.productId!!,
                        quantity = item.quantity,
                        saleUnit = item.saleUnit,
                        skipInventoryDeduction = true,
                    )
                ) {
                    is ApiResult.Success -> successCount++
                    is ApiResult.Failure -> {
                        lastError = result.message
                        failedNames.add(item.productName)
                    }
                }
            }

            val totalLines = validItems.size + beerRounds.size

            if (successCount == 0) {
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    submitError = lastError ?: "Không thể thêm món vào đơn, vui lòng thử lại",
                )
                return@launch
            }

            // Partial failure (order created, some items didn't make it): don't auto-navigate away —
            // that would silently hide a short order from the employee. Surface it and require an
            // explicit tap through to Order Detail, where "Thêm món" can add the missed item(s) manually.
            val warning = if (failedNames.isNotEmpty()) {
                "Đã thêm $successCount/$totalLines dòng. Không thêm được: ${failedNames.joinToString(", ")}. " +
                    "Đơn đã được tạo — mở đơn để thêm thủ công phần còn thiếu."
            } else {
                null
            }

            // Học từ những gì nhân viên đã chốt. Chỉ gửi dòng có chữ gốc khác tên món — tức là
            // trường hợp AI phải suy luận; dòng trùng khít tên món thì không có gì để học.
            val feedback = validItems
                .filter { !it.rawText.isNullOrBlank() && it.rawText != it.productName }
                .map { ScanFeedbackRequest.Item(rawText = it.rawText!!, productId = it.productId!!) }
            if (feedback.isNotEmpty()) {
                // Bỏ qua kết quả: đây là cải thiện dần, không được làm hỏng luồng tạo đơn
                receiptScanRepository.sendFeedback(feedback)
            }

            _uiState.value = _uiState.value.copy(isSubmitting = false, createdOrderId = order.id, submitWarning = warning)
        }
    }

    fun onViewOrder() {
        _uiState.value = _uiState.value.copy(submitWarning = null)
    }

    // Full reset (not just clearing createdOrderId) — if the employee later backs out of the newly
    // created order, they land on a fresh capture screen instead of the already-submitted review list.
    fun onNavigationConsumed() {
        encodedPages.clear()
        _uiState.value = ReceiptScanUiState(tables = _uiState.value.tables)
    }
}
