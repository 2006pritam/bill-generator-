package com.example.billing_app.util

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.billing_app.domain.model.CartItem
import com.example.billing_app.domain.model.Shop
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Random

object PrinterHelper {

    fun generateInvoiceNumber(): String {
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val randomNum = Random().nextInt(9000) + 1000
        return "INV-$dateFormat-$randomNum"
    }

    fun printComputerBill(
        context: Context,
        shop: Shop,
        items: List<CartItem>,
        totalAmount: Double,
        invoiceNo: String = generateInvoiceNumber()
    ) {
        val htmlDocument = generateComputerBillHtml(shop, items, totalAmount, invoiceNo)
        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                val printAdapter = webView.createPrintDocumentAdapter("Bill_$invoiceNo")
                val printAttributes = PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                    .setResolution(PrintAttributes.Resolution("id", "Default", 300, 300))
                    .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                    .build()

                printManager?.print("Computer_Bill_$invoiceNo", printAdapter, printAttributes)
            }
        }
        webView.loadDataWithBaseURL(null, htmlDocument, "text/html", "UTF-8", null)
    }

    fun generateComputerBillHtml(
        shop: Shop,
        items: List<CartItem>,
        totalAmount: Double,
        invoiceNo: String = generateInvoiceNumber()
    ): String {
        val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
        val subtotal = totalAmount
        val totalItemsCount = items.sumOf { it.quantity }

        val itemsHtml = StringBuilder()
        items.forEachIndexed { index, item ->
            itemsHtml.append("""
                <tr>
                    <td style="text-align: center; padding: 10px 8px; border-bottom: 1px solid #e2e8f0; font-size: 13px;">${index + 1}</td>
                    <td style="padding: 10px 8px; border-bottom: 1px solid #e2e8f0; font-size: 13px; font-weight: 600; color: #1e293b;">
                        ${item.product.name}
                        ${if (item.product.barcode.isNotBlank()) "<div style='font-size: 10px; color: #64748b; font-family: monospace;'>Code: ${item.product.barcode}</div>" else ""}
                    </td>
                    <td style="text-align: center; padding: 10px 8px; border-bottom: 1px solid #e2e8f0; font-size: 13px;">${item.quantity}</td>
                    <td style="text-align: right; padding: 10px 8px; border-bottom: 1px solid #e2e8f0; font-size: 13px;">&#8377;${String.format(Locale.US, "%.2f", item.product.price)}</td>
                    <td style="text-align: right; padding: 10px 8px; border-bottom: 1px solid #e2e8f0; font-size: 13px; font-weight: bold; color: #0f172a;">&#8377;${String.format(Locale.US, "%.2f", item.total)}</td>
                </tr>
            """.trimIndent())
        }

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Computer Bill - $invoiceNo</title>
                <style>
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                        color: #1e293b;
                        margin: 0;
                        padding: 24px;
                        background-color: #ffffff;
                    }
                    .invoice-card {
                        max-width: 700px;
                        margin: 0 auto;
                        border: 1px solid #e2e8f0;
                        border-radius: 12px;
                        padding: 28px;
                        box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);
                    }
                    .header {
                        display: flex;
                        justify-content: space-between;
                        align-items: flex-start;
                        border-bottom: 2px solid #6366f1;
                        padding-bottom: 20px;
                        margin-bottom: 20px;
                    }
                    .shop-name {
                        font-size: 24px;
                        font-weight: 800;
                        color: #4338ca;
                        margin-bottom: 4px;
                    }
                    .shop-meta {
                        font-size: 12px;
                        color: #64748b;
                        line-height: 1.5;
                    }
                    .invoice-tag {
                        text-align: right;
                    }
                    .invoice-title {
                        font-size: 18px;
                        font-weight: 800;
                        color: #0f172a;
                        text-transform: uppercase;
                        letter-spacing: 1px;
                    }
                    .meta-row {
                        display: flex;
                        justify-content: space-between;
                        background: #f8fafc;
                        padding: 12px 16px;
                        border-radius: 8px;
                        margin-bottom: 24px;
                        font-size: 13px;
                    }
                    table {
                        width: 100%;
                        border-collapse: collapse;
                        margin-bottom: 24px;
                    }
                    th {
                        background-color: #f1f5f9;
                        color: #475569;
                        font-weight: 700;
                        font-size: 11px;
                        text-transform: uppercase;
                        letter-spacing: 0.5px;
                        padding: 10px 8px;
                        border-bottom: 2px solid #cbd5e1;
                    }
                    .summary-box {
                        display: flex;
                        justify-content: flex-end;
                        margin-bottom: 24px;
                    }
                    .summary-table {
                        width: 280px;
                    }
                    .summary-table td {
                        padding: 6px 0;
                        font-size: 13px;
                    }
                    .grand-total {
                        font-size: 18px;
                        font-weight: 800;
                        color: #4338ca;
                        border-top: 2px solid #e2e8f0;
                        padding-top: 10px !important;
                    }
                    .footer-note {
                        border-top: 1px dashed #cbd5e1;
                        padding-top: 16px;
                        text-align: center;
                        font-size: 12px;
                        color: #64748b;
                    }
                    .computer-badge {
                        display: inline-block;
                        background: #e0e7ff;
                        color: #4338ca;
                        font-size: 10px;
                        font-weight: bold;
                        padding: 3px 8px;
                        border-radius: 4px;
                        margin-top: 8px;
                    }
                    @media print {
                        body { padding: 0; }
                        .invoice-card { border: none; box-shadow: none; padding: 12px; }
                    }
                </style>
            </head>
            <body>
                <div class="invoice-card">
                    <div class="header">
                        <div>
                            <div class="shop-name">${shop.name}</div>
                            <div class="shop-meta">
                                ${if (shop.addressLine1.isNotBlank()) "<div>" + shop.addressLine1 + "</div>" else ""}
                                ${if (shop.addressLine2.isNotBlank()) "<div>" + shop.addressLine2 + "</div>" else ""}
                                ${if (shop.phoneNumber.isNotBlank()) "<div>Phone: " + shop.phoneNumber + "</div>" else ""}
                                ${if (shop.upiId.isNotBlank()) "<div>UPI ID: " + shop.upiId + "</div>" else ""}
                            </div>
                        </div>
                        <div class="invoice-tag">
                            <div class="invoice-title">TAX INVOICE</div>
                            <div style="font-size: 12px; font-weight: 600; color: #4338ca; margin-top: 4px;">$invoiceNo</div>
                            <div class="computer-badge">COMPUTER GENERATED BILL</div>
                        </div>
                    </div>

                    <div class="meta-row">
                        <div><strong>Date & Time:</strong> $dateStr</div>
                        <div><strong>Total Items:</strong> $totalItemsCount</div>
                        <div><strong>Payment Mode:</strong> Cash / UPI</div>
                    </div>

                    <table>
                        <thead>
                            <tr>
                                <th style="width: 40px; text-align: center;">#</th>
                                <th style="text-align: left;">Item Description</th>
                                <th style="width: 60px; text-align: center;">Qty</th>
                                <th style="width: 90px; text-align: right;">Unit Price</th>
                                <th style="width: 100px; text-align: right;">Total</th>
                            </tr>
                        </thead>
                        <tbody>
                            $itemsHtml
                        </tbody>
                    </table>

                    <div class="summary-box">
                        <table class="summary-table">
                            <tr>
                                <td style="color: #64748b;">Subtotal:</td>
                                <td style="text-align: right; font-weight: 600;">&#8377;${String.format(Locale.US, "%.2f", subtotal)}</td>
                            </tr>
                            <tr>
                                <td style="color: #64748b;">Taxes / GST:</td>
                                <td style="text-align: right; font-weight: 600;">Included</td>
                            </tr>
                            <tr class="grand-total">
                                <td>GRAND TOTAL:</td>
                                <td style="text-align: right;">&#8377;${String.format(Locale.US, "%.2f", totalAmount)}</td>
                            </tr>
                        </table>
                    </div>

                    <div class="footer-note">
                        <div>${shop.footerText}</div>
                        <div style="margin-top: 6px; font-size: 10px; color: #94a3b8;">
                            Official Computer Generated Invoice &bull; No signature required &bull; Thank you for your business!
                        </div>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    fun generateReceiptText(
        shop: Shop,
        items: List<CartItem>,
        totalAmount: Double,
        invoiceNo: String = generateInvoiceNumber()
    ): String {
        val dateStr = SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.getDefault()).format(Date())
        val builder = StringBuilder()
        val lineWidth = 34

        fun center(text: String): String {
            if (text.length >= lineWidth) return text
            val padding = (lineWidth - text.length) / 2
            return " ".repeat(padding) + text
        }

        fun divider(): String = "-".repeat(lineWidth)
        fun doubleDivider(): String = "=".repeat(lineWidth)

        builder.appendLine(center(shop.name.uppercase()))
        if (shop.addressLine1.isNotBlank()) builder.appendLine(center(shop.addressLine1))
        if (shop.addressLine2.isNotBlank()) builder.appendLine(center(shop.addressLine2))
        if (shop.phoneNumber.isNotBlank()) builder.appendLine(center("Tel: ${shop.phoneNumber}"))
        builder.appendLine(center("INVOICE: $invoiceNo"))
        builder.appendLine(center(dateStr))
        builder.appendLine(doubleDivider())
        builder.appendLine(String.format(Locale.US, "%-16s %7s %8s", "Item", "Price", "Total"))
        builder.appendLine(divider())

        for (item in items) {
            val itemName = if (item.product.name.length > 15) {
                item.product.name.substring(0, 15)
            } else {
                item.product.name
            }
            val line1 = String.format(
                Locale.US,
                "%-16s %7.2f %8.2f",
                "${item.quantity}x $itemName",
                item.product.price,
                item.total
            )
            builder.appendLine(line1)
        }

        builder.appendLine(divider())
        val totalLine = String.format(Locale.US, "%-16s %15.2f", "GRAND TOTAL (INR):", totalAmount)
        builder.appendLine(totalLine)
        builder.appendLine(doubleDivider())

        if (shop.footerText.isNotBlank()) {
            builder.appendLine(center(shop.footerText))
        }
        builder.appendLine(center("*** COMPUTER GENERATED BILL ***"))
        builder.appendLine()

        return builder.toString()
    }
}

