package com.example.thuhuong_restaurant.feature.employee.order

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thuhuong_restaurant.core.common.ApiResult
import com.example.thuhuong_restaurant.core.model.OrderResponse
import com.example.thuhuong_restaurant.core.model.PaymentMethod
import com.example.thuhuong_restaurant.core.model.PaymentResponse
import com.example.thuhuong_restaurant.core.model.PaymentStatus
import com.example.thuhuong_restaurant.core.model.ProductCategory
import com.example.thuhuong_restaurant.core.model.ProductResponse
import com.example.thuhuong_restaurant.core.model.defaultSaleUnitFor
import com.example.thuhuong_restaurant.core.model.isBeerProduct
import com.example.thuhuong_restaurant.core.model.needsRefund
import com.example.thuhuong_restaurant.core.model.netRefundOf
import com.example.thuhuong_restaurant.feature.employee.payment.PaymentRepository
import com.example.thuhuong_restaurant.feature.menu.MenuRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProductPickerState(
    val products: List<ProductResponse> = emptyList(),
    val isLoading: Boolean = false,
    val selectedCategory: ProductCategory? = null,
    val keyword: String = "",
)

data class SurchargeFormState(
    val name: String = "",
    val unitPrice: String = "",
    val quantity: Int = 1,
    val reason: String = "",
)

data class OrderDetailUiState(
    val order: OrderResponse? = null,
    val payments: List<PaymentResponse> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val isMutating: Boolean = false,
    val mutationError: String? = null,
    val showProductPicker: Boolean = false,
    val picker: ProductPickerState = ProductPickerState(),
    val navigateToPayment: Boolean = false,
    val showSurchargeDialog: Boolean = false,
    val surchargeForm: SurchargeFormState = SurchargeFormState(),
    val isSubmittingSurcharge: Boolean = false,
    val surchargeError: String? = null,
    val showCancelDialog: Boolean = false,
    val cancelReason: String = "",
    val isCancelling: Boolean = false,
    val cancelError: String? = null,
    val navigateBack: Boolean = false,
    val isRefunding: Boolean = false,
    val refundError: String? = null,
    /** Sản phẩm bia cốc trong thực đơn — có thì mới hiện khu vực thêm nhanh. */
    val beerProducts: List<ProductResponse> = emptyList(),
    /** Danh sách đợt bia mặc định thu gọn, giống web — thường có rất nhiều đợt. */
    val showBeerList: Boolean = false,
    val showBeerDialog: Boolean = false,
    val beerQtyInput: String = "",
    val isAddingBeer: Boolean = false,
) {
    val sumCompletedPayments: Double
        get() = payments.filter { it.paymentMethod != PaymentMethod.REFUND && it.status == PaymentStatus.COMPLETED }.sumOf { it.amount }

    val trueRemaining: Double
        get() {
            val o = order ?: return 0.0
            return (o.totalAmount - o.prepaidAmount - sumCompletedPayments).coerceAtLeast(0.0)
        }

    val netRefund: Double
        get() = order?.let { netRefundOf(it.prepaidAmount, it.totalAmount) } ?: 0.0
}

@HiltViewModel
class OrderDetailViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val paymentRepository: PaymentRepository,
    private val menuRepository: MenuRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val orderId: String = checkNotNull(savedStateHandle["orderId"])

    private val _uiState = MutableStateFlow(OrderDetailUiState())
    val uiState: StateFlow<OrderDetailUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadOrder()
        loadBeerProducts()
    }

    fun retry() = loadOrder()

    private fun loadOrder() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = orderRepository.getOrder(orderId)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(order = result.data, isLoading = false)
                    loadPayments()
                }
                is ApiResult.Failure -> _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
            }
        }
    }

    private suspend fun loadPayments() {
        when (val result = paymentRepository.getAllByOrder(orderId)) {
            is ApiResult.Success -> _uiState.value = _uiState.value.copy(payments = result.data)
            is ApiResult.Failure -> Unit
        }
    }

    // ── Bia cốc ─────────────────────────────────────────────────────────────

    private fun loadBeerProducts() {
        viewModelScope.launch {
            val result = menuRepository.getProducts(
                category = ProductCategory.DRINK.name,
                keyword = null,
                page = 0,
                size = 100,
            )
            // Không có bia thì chỉ ẩn khu vực này đi, không phải lỗi chặn màn hình
            if (result is ApiResult.Success) {
                _uiState.value = _uiState.value.copy(
                    beerProducts = result.data.content.filter(::isBeerProduct),
                )
            }
        }
    }

    fun toggleBeerList() {
        _uiState.value = _uiState.value.copy(showBeerList = !_uiState.value.showBeerList)
    }

    fun openBeerDialog() {
        _uiState.value = _uiState.value.copy(showBeerDialog = true, beerQtyInput = "", mutationError = null)
    }

    fun dismissBeerDialog() {
        _uiState.value = _uiState.value.copy(showBeerDialog = false)
    }

    fun appendBeerDigit(digit: String) {
        val current = _uiState.value.beerQtyInput
        if (current.length >= 3) return // 999 cốc một đợt là quá đủ — chặn gõ nhầm dính phím
        val next = if (current == "0") digit else current + digit
        _uiState.value = _uiState.value.copy(beerQtyInput = next)
    }

    fun clearBeerQty() {
        _uiState.value = _uiState.value.copy(beerQtyInput = "")
    }

    /** Mỗi lần thêm là một đợt riêng, không gộp vào đợt cũ — để nhân viên đối chiếu từng lần khách gọi. */
    fun confirmAddBeer() {
        val product = _uiState.value.beerProducts.firstOrNull() ?: return
        val qty = _uiState.value.beerQtyInput.toIntOrNull() ?: return
        if (qty < 1) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAddingBeer = true, mutationError = null)
            when (val result = orderRepository.addItem(orderId, product.id, qty, "CUP")) {
                is ApiResult.Success -> _uiState.value = _uiState.value.copy(
                    order = result.data,
                    isAddingBeer = false,
                    showBeerDialog = false,
                    beerQtyInput = "",
                    showBeerList = true,
                )
                is ApiResult.Failure -> _uiState.value = _uiState.value.copy(
                    isAddingBeer = false,
                    mutationError = result.message,
                )
            }
        }
    }

    fun openProductPicker() {
        _uiState.value = _uiState.value.copy(showProductPicker = true)
        loadPickerProducts()
    }

    fun closeProductPicker() {
        _uiState.value = _uiState.value.copy(showProductPicker = false)
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
                is ApiResult.Failure -> _uiState.value = _uiState.value.copy(
                    picker = _uiState.value.picker.copy(isLoading = false),
                    mutationError = result.message,
                )
            }
        }
    }

    fun selectProduct(product: ProductResponse) {
        val order = _uiState.value.order ?: return
        val saleUnit = defaultSaleUnitFor(product)
        val existing = order.items.find { it.productId == product.id && it.saleUnit == saleUnit }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isMutating = true, mutationError = null)
            val result = if (existing != null) {
                orderRepository.updateItemQuantity(orderId, existing.id, existing.quantity + 1)
            } else {
                orderRepository.addItem(orderId, product.id, 1, saleUnit)
            }
            applyMutationResult(result)
        }
    }

    fun adjustQuantity(itemId: String, currentQuantity: Int, delta: Int) {
        val newQuantity = currentQuantity + delta
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isMutating = true, mutationError = null)
            val result = if (newQuantity <= 0) {
                orderRepository.removeItem(orderId, itemId)
            } else {
                orderRepository.updateItemQuantity(orderId, itemId, newQuantity)
            }
            applyMutationResult(result)
        }
    }

    fun removeItem(itemId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isMutating = true, mutationError = null)
            applyMutationResult(orderRepository.removeItem(orderId, itemId))
        }
    }

    private fun applyMutationResult(result: ApiResult<OrderResponse>) {
        when (result) {
            is ApiResult.Success -> _uiState.value = _uiState.value.copy(order = result.data, isMutating = false)
            is ApiResult.Failure -> _uiState.value = _uiState.value.copy(isMutating = false, mutationError = result.message)
        }
    }

    fun onPaymentClick() {
        _uiState.value = _uiState.value.copy(navigateToPayment = true)
    }

    fun onNavigationConsumed() {
        _uiState.value = _uiState.value.copy(navigateToPayment = false)
    }

    // ── Surcharges ──────────────────────────────────────────────────────────

    fun openSurchargeDialog() {
        _uiState.value = _uiState.value.copy(showSurchargeDialog = true, surchargeForm = SurchargeFormState(), surchargeError = null)
    }

    fun dismissSurchargeDialog() {
        _uiState.value = _uiState.value.copy(showSurchargeDialog = false, surchargeError = null)
    }

    fun updateSurchargeName(name: String) {
        _uiState.value = _uiState.value.copy(surchargeForm = _uiState.value.surchargeForm.copy(name = name))
    }

    fun updateSurchargeUnitPrice(unitPrice: String) {
        _uiState.value = _uiState.value.copy(surchargeForm = _uiState.value.surchargeForm.copy(unitPrice = unitPrice))
    }

    fun updateSurchargeQuantity(quantity: Int) {
        if (quantity < 1) return
        _uiState.value = _uiState.value.copy(surchargeForm = _uiState.value.surchargeForm.copy(quantity = quantity))
    }

    fun updateSurchargeReason(reason: String) {
        _uiState.value = _uiState.value.copy(surchargeForm = _uiState.value.surchargeForm.copy(reason = reason))
    }

    fun confirmAddSurcharge() {
        val form = _uiState.value.surchargeForm
        val unitPrice = form.unitPrice.toDoubleOrNull()
        if (form.name.isBlank() || unitPrice == null || unitPrice < 0) {
            _uiState.value = _uiState.value.copy(surchargeError = "Vui lòng nhập đầy đủ tên và đơn giá hợp lệ")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmittingSurcharge = true, surchargeError = null)
            when (
                val result = orderRepository.addSurcharge(
                    orderId, form.name, unitPrice, form.quantity, form.reason.takeIf { it.isNotBlank() },
                )
            ) {
                is ApiResult.Success -> _uiState.value = _uiState.value.copy(
                    order = result.data,
                    isSubmittingSurcharge = false,
                    showSurchargeDialog = false,
                )
                is ApiResult.Failure -> _uiState.value = _uiState.value.copy(isSubmittingSurcharge = false, surchargeError = result.message)
            }
        }
    }

    fun removeSurcharge(surchargeId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isMutating = true, mutationError = null)
            applyMutationResult(orderRepository.removeSurcharge(orderId, surchargeId))
        }
    }

    // ── Cancel ──────────────────────────────────────────────────────────────

    fun openCancelDialog() {
        _uiState.value = _uiState.value.copy(showCancelDialog = true, cancelReason = "", cancelError = null)
    }

    fun dismissCancelDialog() {
        _uiState.value = _uiState.value.copy(showCancelDialog = false, cancelError = null)
    }

    fun updateCancelReason(reason: String) {
        _uiState.value = _uiState.value.copy(cancelReason = reason)
    }

    fun confirmCancel() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCancelling = true, cancelError = null)
            when (val result = orderRepository.cancelOrder(orderId, _uiState.value.cancelReason.takeIf { it.isNotBlank() })) {
                is ApiResult.Success -> _uiState.value = _uiState.value.copy(isCancelling = false, showCancelDialog = false, navigateBack = true)
                is ApiResult.Failure -> _uiState.value = _uiState.value.copy(isCancelling = false, cancelError = result.message)
            }
        }
    }

    // ── Refund ──────────────────────────────────────────────────────────────

    fun confirmRefund() {
        val netRefund = _uiState.value.netRefund
        if (netRefund <= 0) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefunding = true, refundError = null)
            when (val result = paymentRepository.refund(orderId, amount = netRefund)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(isRefunding = false)
                    loadOrder()
                }
                is ApiResult.Failure -> _uiState.value = _uiState.value.copy(isRefunding = false, refundError = result.message)
            }
        }
    }
}
