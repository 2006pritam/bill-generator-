package com.example.billing_app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Product(
    val id: String,
    val name: String,
    val barcode: String,
    val price: Double,
    val stock: Int = 0
)

@Serializable
data class CartItem(
    val product: Product,
    val quantity: Int = 1
) {
    val total: Double
        get() = product.price * quantity
}

@Serializable
data class Shop(
    val id: Int = 1,
    val name: String = "Elite Groceries",
    val addressLine1: String = "123 Market Street",
    val addressLine2: String = "Central City, 560001",
    val phoneNumber: String = "+91 9876543210",
    val upiId: String = "elitegroceries@upi",
    val footerText: String = "Thank you for shopping with us! Visit again."
)

data class BluetoothDeviceItem(
    val name: String,
    val address: String,
    val isConnected: Boolean = false,
    val isBonded: Boolean = true
)
