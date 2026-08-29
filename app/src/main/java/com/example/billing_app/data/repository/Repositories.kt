package com.example.billing_app.data.repository

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.SharedPreferences
import com.example.billing_app.data.db.ProductDao
import com.example.billing_app.data.db.ProductEntity
import com.example.billing_app.data.db.ShopDao
import com.example.billing_app.data.db.ShopEntity
import com.example.billing_app.domain.model.BluetoothDeviceItem
import com.example.billing_app.domain.model.Product
import com.example.billing_app.domain.model.Shop
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import java.util.UUID

class ProductRepository(private val productDao: ProductDao) {

    val allProducts: Flow<List<Product>> = productDao.getAllProducts().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun getProductByBarcode(barcode: String): Product? {
        return productDao.getProductByBarcode(barcode.trim())?.toDomain()
    }

    suspend fun getProductById(id: String): Product? {
        return productDao.getProductById(id)?.toDomain()
    }

    suspend fun addProduct(product: Product) {
        productDao.insertProduct(ProductEntity.fromDomain(product))
    }

    suspend fun updateProduct(product: Product) {
        productDao.updateProduct(ProductEntity.fromDomain(product))
    }

    suspend fun deleteProduct(id: String) {
        productDao.deleteProductById(id)
    }

    suspend fun deleteAllProducts() {
        productDao.deleteAllProducts()
    }

    suspend fun seedInitialProductsIfEmpty() {
        // No automatic old/dummy products seeding - keep catalog clean for fresh user data only
    }
}

class ShopRepository(private val shopDao: ShopDao) {

    val shopDetails: Flow<Shop> = shopDao.getShopDetails().map { entity ->
        entity?.toDomain() ?: Shop()
    }

    suspend fun getShopDetailsOnce(): Shop {
        return shopDao.getShopDetailsOnce()?.toDomain() ?: Shop()
    }

    suspend fun saveShopDetails(shop: Shop) {
        shopDao.saveShopDetails(ShopEntity.fromDomain(shop))
    }

    suspend fun seedDefaultShopIfEmpty() {
        val current = shopDao.getShopDetailsOnce()
        if (current == null) {
            shopDao.saveShopDetails(ShopEntity.fromDomain(Shop()))
        }
    }
}

class PrinterRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("printer_prefs", Context.MODE_PRIVATE)

    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private val _connectedDevice = MutableStateFlow<BluetoothDeviceItem?>(null)
    val connectedDevice: StateFlow<BluetoothDeviceItem?> = _connectedDevice.asStateFlow()

    private val _isConnecting = MutableStateFlow(false)
    val isConnecting: StateFlow<Boolean> = _isConnecting.asStateFlow()

    fun getSavedPrinterMac(): String? = prefs.getString("printer_mac", null)
    fun getSavedPrinterName(): String? = prefs.getString("printer_name", null)

    fun savePrinter(name: String, mac: String) {
        prefs.edit().putString("printer_mac", mac).putString("printer_name", name).apply()
    }

    fun clearSavedPrinter() {
        prefs.edit().remove("printer_mac").remove("printer_name").apply()
        _connectedDevice.value = null
    }

    fun getBondedDevices(): List<BluetoothDeviceItem> {
        val list = mutableListOf<BluetoothDeviceItem>()
        try {
            val bonded = bluetoothAdapter?.bondedDevices
            if (bonded != null && bonded.isNotEmpty()) {
                val savedMac = getSavedPrinterMac()
                for (device: BluetoothDevice in bonded) {
                    list.add(
                        BluetoothDeviceItem(
                            name = device.name ?: "Bluetooth Device",
                            address = device.address,
                            isConnected = device.address == savedMac,
                            isBonded = true
                        )
                    )
                }
            }
        } catch (_: SecurityException) {
            // Missing Bluetooth permission
        }

        // Add standard thermal printer presets for testing / emulator environments
        val savedMac = getSavedPrinterMac()
        val defaultPrinters = listOf(
            BluetoothDeviceItem("POS-58 Thermal Printer", "00:11:22:33:44:55", isConnected = savedMac == "00:11:22:33:44:55"),
            BluetoothDeviceItem("RPP02N Mobile POS", "AA:BB:CC:DD:EE:FF", isConnected = savedMac == "AA:BB:CC:DD:EE:FF"),
            BluetoothDeviceItem("Sunmi V2 Thermal Print", "12:34:56:78:9A:BC", isConnected = savedMac == "12:34:56:78:9A:BC")
        )

        for (preset in defaultPrinters) {
            if (list.none { it.address == preset.address }) {
                list.add(preset)
            }
        }

        return list
    }

    suspend fun connectDevice(device: BluetoothDeviceItem): Boolean {
        _isConnecting.value = true
        kotlinx.coroutines.delay(600) // connection handshake simulation
        savePrinter(device.name, device.address)
        _connectedDevice.value = device.copy(isConnected = true)
        _isConnecting.value = false
        return true
    }

    fun disconnect() {
        clearSavedPrinter()
    }
}
