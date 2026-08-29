package com.example.billing_app.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.billing_app.ui.components.PrimaryButton
import com.example.billing_app.ui.theme.PrimaryPurple
import com.example.billing_app.ui.theme.SuccessGreen
import com.example.billing_app.ui.theme.TextPrimary
import com.example.billing_app.ui.theme.TextSecondary
import com.example.billing_app.ui.viewmodel.BillingViewModel
import com.example.billing_app.ui.viewmodel.ShopViewModel
import com.example.billing_app.util.PrinterHelper
import com.example.billing_app.util.QrCodeGenerator
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    billingViewModel: BillingViewModel,
    shopViewModel: ShopViewModel,
    onNavigateBack: () -> Unit
) {
    val billingState by billingViewModel.uiState.collectAsStateWithLifecycle()
    val shop by shopViewModel.shop.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var showInvoicePreview by remember { mutableStateOf(false) }
    val currentInvoiceNo = remember { PrinterHelper.generateInvoiceNumber() }
    val currentDateStr = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date()) }

    BackHandler {
        billingViewModel.clearCart()
        onNavigateBack()
    }

    LaunchedEffect(billingState.printSuccess) {
        if (billingState.printSuccess) {
            snackbarHostState.showSnackbar("Computer Bill generated & printed successfully!")
        }
    }

    val upiQrContent = remember(shop.upiId, shop.name, billingState.totalAmount) {
        if (shop.upiId.isNotBlank()) {
            val encodedName = URLEncoder.encode(shop.name, StandardCharsets.UTF_8.toString())
            "upi://pay?pa=${shop.upiId}&pn=$encodedName&am=${String.format("%.2f", billingState.totalAmount)}&cu=INR"
        } else ""
    }

    val qrBitmap = remember(upiQrContent) {
        if (upiQrContent.isNotBlank()) {
            QrCodeGenerator.generateQrBitmap(upiQrContent, 400)
        } else null
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Checkout",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            billingViewModel.clearCart()
                            onNavigateBack()
                        },
                        modifier = Modifier.testTag("checkout_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = PrimaryPurple
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val receiptText = PrinterHelper.generateReceiptText(
                                shop = shop,
                                items = billingState.cartItems,
                                totalAmount = billingState.totalAmount,
                                invoiceNo = currentInvoiceNo
                            )
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, receiptText)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Computer Bill"))
                        },
                        modifier = Modifier.testTag("share_receipt_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = "Share Receipt",
                            tint = PrimaryPurple
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFFF8FAFC)
                )
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(16.dp),
                color = Color.White
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "GRAND TOTAL",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextSecondary,
                                fontSize = 12.sp,
                                letterSpacing = 1.2.sp
                            )
                        )
                        Text(
                            text = "₹${String.format("%.2f", billingState.totalAmount)}",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 26.sp,
                                color = TextPrimary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showInvoicePreview = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("preview_bill_button"),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, PrimaryPurple)
                        ) {
                            Icon(Icons.Filled.Description, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("View Bill", color = PrimaryPurple, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }

                        PrimaryButton(
                            text = if (billingState.isPrinting) "Printing..." else "Print Bill (PDF)",
                            icon = Icons.Filled.Print,
                            isLoading = billingState.isPrinting,
                            modifier = Modifier.weight(1.3f),
                            onClick = {
                                PrinterHelper.printComputerBill(
                                    context = context,
                                    shop = shop,
                                    items = billingState.cartItems,
                                    totalAmount = billingState.totalAmount,
                                    invoiceNo = currentInvoiceNo
                                )
                                billingViewModel.printReceipt(shop)
                            },
                            testTag = "print_receipt_button"
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFC))
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Invoice Metadata Header
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = PrimaryPurple.copy(alpha = 0.07f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "COMPUTER BILL NO.",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryPurple,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = currentInvoiceNo,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = TextPrimary
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = PrimaryPurple,
                        contentColor = Color.White
                    ) {
                        Text(
                            text = "READY",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            // Itemized Bill Table Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Table Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF1F5F9))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "PRODUCT NAME",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextSecondary,
                                fontSize = 10.sp,
                                letterSpacing = 1.sp
                            ),
                            modifier = Modifier.weight(1.8f)
                        )
                        Text(
                            text = "PRICE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextSecondary,
                                fontSize = 10.sp,
                                textAlign = TextAlign.End,
                                letterSpacing = 1.sp
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "TOTAL",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextSecondary,
                                fontSize = 10.sp,
                                textAlign = TextAlign.End,
                                letterSpacing = 1.sp
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Divider(color = Color(0xFFE2E8F0), thickness = 1.dp)

                    // Table Rows
                    billingState.cartItems.forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${item.quantity}x ${item.product.name}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary
                                ),
                                modifier = Modifier.weight(1.8f)
                            )
                            Text(
                                text = "₹${String.format("%.2f", item.product.price)}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = TextSecondary,
                                    textAlign = TextAlign.End
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "₹${String.format("%.2f", item.total)}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    textAlign = TextAlign.End
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (index < billingState.cartItems.size - 1) {
                            Divider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Dynamic UPI Payment QR Code Card
            if (shop.upiId.isNotBlank() && qrBitmap != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("upi_qr_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Scan to Pay",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "UPI: ${shop.upiId}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        Box(
                            modifier = Modifier
                                .size(200.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = qrBitmap.asImageBitmap(),
                                contentDescription = "UPI Payment QR Code",
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Accept payments via GPay, PhonePe, Paytm, or any UPI app",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Computer Bill Preview Dialog
    if (showInvoicePreview) {
        AlertDialog(
            onDismissRequest = { showInvoicePreview = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Computer Generated Bill",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextPrimary
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(text = shop.name, fontWeight = FontWeight.Black, fontSize = 20.sp, color = PrimaryPurple)
                    if (shop.addressLine1.isNotBlank()) Text(text = shop.addressLine1, fontSize = 12.sp, color = TextSecondary)
                    if (shop.phoneNumber.isNotBlank()) Text(text = "Tel: ${shop.phoneNumber}", fontSize = 12.sp, color = TextSecondary)
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Invoice: $currentInvoiceNo", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text(text = currentDateStr, fontSize = 11.sp, color = TextSecondary)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    billingState.cartItems.forEachIndexed { idx, itm ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "${idx + 1}. ${itm.product.name} (x${itm.quantity})", fontSize = 13.sp, color = TextPrimary, modifier = Modifier.weight(2f))
                            Text(text = "₹${String.format("%.2f", itm.total)}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(thickness = 1.5.dp, color = PrimaryPurple)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Grand Total:", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(text = "₹${String.format("%.2f", billingState.totalAmount)}", fontWeight = FontWeight.Black, fontSize = 18.sp, color = PrimaryPurple)
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFF1F5F9),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "*** Official Computer Generated Bill - Valid for All Transactions ***",
                            fontSize = 10.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        PrinterHelper.printComputerBill(
                            context = context,
                            shop = shop,
                            items = billingState.cartItems,
                            totalAmount = billingState.totalAmount,
                            invoiceNo = currentInvoiceNo
                        )
                        showInvoicePreview = false
                    }
                ) {
                    Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Print / Save PDF", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showInvoicePreview = false }) {
                    Text("Close")
                }
            }
        )
    }
}
