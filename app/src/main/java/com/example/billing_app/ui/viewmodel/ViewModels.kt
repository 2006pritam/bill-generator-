package com.example.billing_app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.billing_app.data.repository.PrinterRepository
import com.example.billing_app.data.repository.ProductRepository
import com.example.billing_app.data.repository.ShopRepository
import com.example.billing_app.domain.model.BluetoothDeviceItem
import com.example.billing_app.domain.model.CartItem
import com.example.billing_app.domain.model.Product
import com.example.billing_app.domain.model.Shop
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class BillingUiState(
    val cartItems: List<CartItem> = emptyList(),
    val totalAmount: Double = 0.0,
    val isPrinting: Boolean = false,
    val printSuccess: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val lastScannedBarcode: String? = null
)

class BillingViewModel(
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BillingUiState())
    val uiState: StateFlow<BillingUiState> = _uiState.asStateFlow()

    private var lastScanTime = 0L
    private var lastScannedCode = ""

    fun scanBarcode(barcode: String, onProductFound: (() -> Unit)? = null) {
        val now = System.currentTimeMillis()
        if (barcode == lastScannedCode && (now - lastScanTime) < 1800) {
            return // Cooldown to prevent duplicate fast scans
        }
        lastScanTime = now
        lastScannedCode = barcode

        viewModelScope.launch {
            val product = productRepository.getProductByBarcode(barcode)
            if (product != null) {
                addProductToCart(product)
                onProductFound?.invoke()
            } else {
                _uiState.update {
                    it.copy(
                        errorMessage = "Product not found for barcode: $barcode",
                        lastScannedBarcode = barcode
                    )
                }
            }
        }
    }

    fun addProductToCart(product: Product) {
        _uiState.update { state ->
            val existingIndex = state.cartItems.indexOfFirst { it.product.id == product.id }
            val updatedList = if (existingIndex >= 0) {
                state.cartItems.toMutableList().apply {
                    val current = this[existingIndex]
                    this[existingIndex] = current.copy(quantity = current.quantity + 1)
                }
            } else {
                state.cartItems + CartItem(product = product, quantity = 1)
            }
            val total = updatedList.sumOf { it.total }
            state.copy(
                cartItems = updatedList,
                totalAmount = total,
                errorMessage = null,
                infoMessage = "Added ${product.name} to cart"
            )
        }
    }

    fun removeProductFromCart(productId: String) {
        _uiState.update { state ->
            val updatedList = state.cartItems.filterNot { it.product.id == productId }
            val total = updatedList.sumOf { it.total }
            state.copy(
                cartItems = updatedList,
                totalAmount = total
            )
        }
    }

    fun updateQuantity(productId: String, newQuantity: Int) {
        if (newQuantity <= 0) {
            removeProductFromCart(productId)
            return
        }
        _uiState.update { state ->
            val updatedList = state.cartItems.map { item ->
                if (item.product.id == productId) item.copy(quantity = newQuantity) else item
            }
            val total = updatedList.sumOf { it.total }
            state.copy(
                cartItems = updatedList,
                totalAmount = total
            )
        }
    }

    fun clearCart() {
        _uiState.update {
            BillingUiState()
        }
    }

    fun printReceipt(shop: Shop) {
        viewModelScope.launch {
            _uiState.update { it.copy(isPrinting = true, printSuccess = false, errorMessage = null) }
            kotlinx.coroutines.delay(1000) // Printing simulation / delay
            _uiState.update { it.copy(isPrinting = false, printSuccess = true) }
        }
    }

    fun dismissMessage() {
        _uiState.update { it.copy(errorMessage = null, infoMessage = null) }
    }
}

data class ProductUiState(
    val products: List<Product> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

class ProductViewModel(
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _message = MutableStateFlow<String?>(null)
    private val _error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ProductUiState> = combine(
        productRepository.allProducts,
        _searchQuery,
        _message,
        _error
    ) { products, query, msg, err ->
        val filtered = if (query.isBlank()) {
            products
        } else {
            products.filter {
                it.name.contains(query, ignoreCase = true) ||
                it.barcode.contains(query, ignoreCase = true)
            }
        }
        ProductUiState(
            products = filtered,
            searchQuery = query,
            message = msg,
            error = err
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProductUiState(isLoading = true)
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addProduct(
        name: String,
        barcode: String,
        price: Double,
        stock: Int,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val existing = productRepository.getProductByBarcode(barcode.trim())
            if (existing != null) {
                _error.value = "Product with barcode '$barcode' already exists!"
                return@launch
            }
            val product = Product(
                id = UUID.randomUUID().toString(),
                name = name.trim(),
                barcode = barcode.trim(),
                price = price,
                stock = stock
            )
            productRepository.addProduct(product)
            _message.value = "Product added successfully!"
            onSuccess()
        }
    }

    fun updateProduct(product: Product, onSuccess: () -> Unit) {
        viewModelScope.launch {
            productRepository.updateProduct(product)
            _message.value = "Product updated successfully!"
            onSuccess()
        }
    }

    fun deleteProduct(id: String) {
        viewModelScope.launch {
            productRepository.deleteProduct(id)
            _message.value = "Product removed."
        }
    }

    fun dismissMessage() {
        _message.value = null
        _error.value = null
    }
}

class ShopViewModel(
    private val shopRepository: ShopRepository
) : ViewModel() {

    val shop: StateFlow<Shop> = shopRepository.shopDetails
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Shop()
        )

    private val _savedEvent = MutableStateFlow(false)
    val savedEvent: StateFlow<Boolean> = _savedEvent.asStateFlow()

    fun updateShop(
        name: String,
        address1: String,
        address2: String,
        phone: String,
        upi: String,
        footer: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val updated = Shop(
                id = 1,
                name = name.trim(),
                addressLine1 = address1.trim(),
                addressLine2 = address2.trim(),
                phoneNumber = phone.trim(),
                upiId = upi.trim(),
                footerText = footer.trim()
            )
            shopRepository.saveShopDetails(updated)
            _savedEvent.value = true
            onSuccess()
        }
    }

    fun resetSavedEvent() {
        _savedEvent.value = false
    }
}

class PrinterViewModel(
    private val printerRepository: PrinterRepository
) : ViewModel() {

    private val _devices = MutableStateFlow<List<BluetoothDeviceItem>>(emptyList())
    val devices: StateFlow<List<BluetoothDeviceItem>> = _devices.asStateFlow()

    val connectedDevice: StateFlow<BluetoothDeviceItem?> = printerRepository.connectedDevice
    val isConnecting: StateFlow<Boolean> = printerRepository.isConnecting

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        refreshDevices()
    }

    fun refreshDevices() {
        _devices.value = printerRepository.getBondedDevices()
    }

    fun connect(device: BluetoothDeviceItem) {
        viewModelScope.launch {
            val success = printerRepository.connectDevice(device)
            if (success) {
                _message.value = "Connected to ${device.name}"
                refreshDevices()
            } else {
                _message.value = "Failed to connect to ${device.name}"
            }
        }
    }

    fun disconnect() {
        printerRepository.disconnect()
        _message.value = "Printer disconnected"
        refreshDevices()
    }

    fun dismissMessage() {
        _message.value = null
    }
}

class AppViewModelFactory(
    private val productRepository: ProductRepository,
    private val shopRepository: ShopRepository,
    private val printerRepository: PrinterRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(BillingViewModel::class.java) ->
                BillingViewModel(productRepository) as T
            modelClass.isAssignableFrom(ProductViewModel::class.java) ->
                ProductViewModel(productRepository) as T
            modelClass.isAssignableFrom(ShopViewModel::class.java) ->
                ShopViewModel(shopRepository) as T
            modelClass.isAssignableFrom(PrinterViewModel::class.java) ->
                PrinterViewModel(printerRepository) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
