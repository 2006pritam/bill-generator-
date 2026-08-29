package com.example.billing_app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.billing_app.domain.model.CartItem
import com.example.billing_app.ui.components.AppTextField
import com.example.billing_app.ui.components.CameraBarcodeScanner
import com.example.billing_app.ui.components.PrimaryButton
import com.example.billing_app.ui.theme.PrimaryPurple
import com.example.billing_app.ui.theme.TextPrimary
import com.example.billing_app.ui.theme.TextSecondary
import com.example.billing_app.ui.viewmodel.BillingViewModel

@Composable
fun HomeScreen(
    billingViewModel: BillingViewModel,
    onNavigateToCheckout: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAddProductWithBarcode: (String) -> Unit = {}
) {
    val uiState by billingViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            billingViewModel.dismissMessage()
        }
    }

    var isCameraOn by remember { mutableStateOf(true) }
    var showManualInputDialog by remember { mutableStateOf(false) }
    var manualBarcode by remember { mutableStateOf("") }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(16.dp),
                color = Color.White
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    PrimaryButton(
                        text = "Review Order (${uiState.cartItems.sumOf { it.quantity }})",
                        icon = Icons.Filled.Payment,
                        onClick = onNavigateToCheckout,
                        enabled = uiState.cartItems.isNotEmpty(),
                        testTag = "review_order_button"
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top 38% Scanner Viewport
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.38f)
                        .background(Color.Black)
                ) {
                    CameraBarcodeScanner(
                        onBarcodeDetected = { scannedCode ->
                            billingViewModel.scanBarcode(scannedCode)
                        },
                        isCameraEnabled = isCameraOn && hasCameraPermission,
                        onToggleCamera = { isCameraOn = !isCameraOn },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Top Bar Overlay Buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                            modifier = Modifier.size(44.dp)
                        ) {
                            IconButton(
                                onClick = { showManualInputDialog = true },
                                modifier = Modifier.testTag("manual_barcode_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Keyboard,
                                    contentDescription = "Manual barcode input",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                            modifier = Modifier.size(44.dp)
                        ) {
                            IconButton(
                                onClick = onNavigateToSettings,
                                modifier = Modifier.testTag("settings_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Settings,
                                    contentDescription = "Settings",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }

                // Bottom 62% Cart Items List Panel
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.62f),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    color = Color(0xFFF8FAFC)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 10.dp)
                    ) {
                        // Drag Indicator Bar
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .size(width = 44.dp, height = 4.dp)
                                .background(Color(0xFFCBD5E1), RoundedCornerShape(2.dp))
                        )

                        // Header (Scanned Items count + Total Price)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Scanned Items",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = TextPrimary
                                    )
                                )
                                Text(
                                    text = "${uiState.cartItems.sumOf { it.quantity }} items total",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "TOTAL PRICE",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = TextSecondary,
                                        fontSize = 10.sp,
                                        letterSpacing = 1.2.sp
                                    )
                                )
                                Text(
                                    text = "₹${String.format("%.2f", uiState.totalAmount)}",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Black,
                                        fontSize = 22.sp,
                                        color = PrimaryPurple
                                    )
                                )
                            }
                        }

                        Divider(color = Color(0xFFE2E8F0), thickness = 1.dp)

                        // Cart Items List or Empty State
                        if (uiState.cartItems.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFFE2E8F0).copy(alpha = 0.5f),
                                        modifier = Modifier.size(80.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Filled.ShoppingBasket,
                                                contentDescription = null,
                                                tint = Color(0xFF94A3B8),
                                                modifier = Modifier.size(40.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Cart is empty",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Scan items using the camera above or tap the keyboard icon to type barcodes manually.",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = TextSecondary,
                                            textAlign = TextAlign.Center,
                                            fontSize = 13.sp
                                        ),
                                        modifier = Modifier.padding(horizontal = 24.dp)
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(uiState.cartItems, key = { it.product.id }) { item ->
                                    CartItemCard(
                                        item = item,
                                        onIncrease = {
                                            billingViewModel.updateQuantity(item.product.id, item.quantity + 1)
                                        },
                                        onDecrease = {
                                            billingViewModel.updateQuantity(item.product.id, item.quantity - 1)
                                        }
                                    )
                                }
                                item {
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Manual Barcode Input Dialog
    if (showManualInputDialog) {
        AlertDialog(
            onDismissRequest = { showManualInputDialog = false },
            title = {
                Text(
                    text = "Manual Barcode Entry",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter or paste the barcode number to look up and add the product:",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    AppTextField(
                        value = manualBarcode,
                        onValueChange = { manualBarcode = it },
                        placeholder = "e.g. 8901030383424",
                        leadingIcon = Icons.Filled.Keyboard,
                        testTag = "manual_barcode_input"
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (manualBarcode.isNotBlank()) {
                            billingViewModel.scanBarcode(manualBarcode.trim())
                            manualBarcode = ""
                            showManualInputDialog = false
                        }
                    },
                    modifier = Modifier.testTag("submit_manual_barcode_button")
                ) {
                    Text("Add to Cart", fontWeight = FontWeight.Bold, color = PrimaryPurple)
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualInputDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Unregistered Barcode Scanned Prompt Dialog
    uiState.unregisteredBarcode?.let { unknownCode ->
        AlertDialog(
            onDismissRequest = { billingViewModel.clearUnregisteredBarcode() },
            title = {
                Text(
                    text = "Product Not Found",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "This barcode is not registered in your inventory:",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = PrimaryPurple.copy(alpha = 0.08f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryPurple.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = unknownCode,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = PrimaryPurple
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Would you like to add a new product with this barcode number?",
                        fontSize = 13.sp,
                        color = TextPrimary
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val codeToAdd = unknownCode
                        billingViewModel.clearUnregisteredBarcode()
                        onNavigateToAddProductWithBarcode(codeToAdd)
                    },
                    modifier = Modifier.testTag("add_scanned_product_button")
                ) {
                    Text("Add Product", fontWeight = FontWeight.Bold, color = PrimaryPurple)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { billingViewModel.clearUnregisteredBarcode() },
                    modifier = Modifier.testTag("dismiss_unregistered_barcode_button")
                ) {
                    Text("Dismiss", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
fun CartItemCard(
    item: CartItem,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("cart_item_${item.product.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.product.name,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "₹${String.format("%.2f", item.product.price)}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "• Total: ₹${String.format("%.2f", item.total)}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = PrimaryPurple
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Quantity Increment/Decrement Stepper
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFF1F5F9)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(2.dp)
                ) {
                    IconButton(
                        onClick = onDecrease,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("decrease_qty_${item.product.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Remove,
                            contentDescription = "Decrease Quantity",
                            tint = TextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "${item.quantity}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(28.dp)
                    )
                    IconButton(
                        onClick = onIncrease,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("increase_qty_${item.product.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Increase Quantity",
                            tint = TextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
