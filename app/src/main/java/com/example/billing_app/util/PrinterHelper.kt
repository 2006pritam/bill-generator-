package com.example.billing_app.util

import com.example.billing_app.domain.model.CartItem
import com.example.billing_app.domain.model.Shop
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PrinterHelper {

    fun generateReceiptText(
        shop: Shop,
        items: List<CartItem>,
        totalAmount: Double
    ): String {
        val dateStr = SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.getDefault()).format(Date())
        val builder = StringBuilder()
        val lineWidth = 32

        fun center(text: String): String {
            if (text.length >= lineWidth) return text
            val padding = (lineWidth - text.length) / 2
            return " ".repeat(padding) + text
        }

        fun divider(): String = "-".repeat(lineWidth)

        builder.appendLine(center(shop.name.uppercase()))
        if (shop.addressLine1.isNotBlank()) builder.appendLine(center(shop.addressLine1))
        if (shop.addressLine2.isNotBlank()) builder.appendLine(center(shop.addressLine2))
        if (shop.phoneNumber.isNotBlank()) builder.appendLine(center("Tel: ${shop.phoneNumber}"))
        builder.appendLine(center(dateStr))
        builder.appendLine(divider())
        builder.appendLine(String.format(Locale.US, "%-16s %7s %7s", "Item", "Price", "Total"))
        builder.appendLine(divider())

        for (item in items) {
            val itemName = if (item.product.name.length > 15) {
                item.product.name.substring(0, 15)
            } else {
                item.product.name
            }
            val line1 = String.format(
                Locale.US,
                "%-16s %7.2f %7.2f",
                "${item.quantity}x $itemName",
                item.product.price,
                item.total
            )
            builder.appendLine(line1)
        }

        builder.appendLine(divider())
        val totalLine = String.format(Locale.US, "%-16s %14.2f", "TOTAL (INR):", totalAmount)
        builder.appendLine(totalLine)
        builder.appendLine(divider())

        if (shop.footerText.isNotBlank()) {
            builder.appendLine()
            builder.appendLine(center(shop.footerText))
        }
        builder.appendLine()

        return builder.toString()
    }
}
