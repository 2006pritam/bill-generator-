package com.example.billing_app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.billing_app.domain.model.Product
import com.example.billing_app.ui.components.AppTextField
import com.example.billing_app.ui.components.InputLabel
import com.example.billing_app.ui.components.PrimaryButton
import com.example.billing_app.ui.theme.PrimaryPurple
import com.example.billing_app.ui.theme.TextPrimary
import com.example.billing_app.ui.theme.TextSecondary
import com.example.billing_app.ui.viewmodel.ProductViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditProductScreen(
    productViewModel: ProductViewModel,
    existingProduct: Product? = null,
    scannedBarcode: String? = null,
    onOpenScanner: () -> Unit,
    onNavigateBack: () -> Unit
) {
    var name by remember { mutableStateOf(existingProduct?.name ?: "") }
    var barcode by remember { mutableStateOf(existingProduct?.barcode ?: (scannedBarcode ?: "")) }
    var priceStr by remember { mutableStateOf(existingProduct?.let { String.format("%.2f", it.price) } ?: "") }
    var stockStr by remember { mutableStateOf(existingProduct?.let { it.stock.toString() } ?: "50") }

    var nameError by remember { mutableStateOf<String?>(null) }
    var barcodeError by remember { mutableStateOf<String?>(null) }
    var priceError by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val isEditing = existingProduct != null

    LaunchedEffect(scannedBarcode) {
        if (!scannedBarcode.isNullOrBlank() && !isEditing) {
            barcode = scannedBarcode
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (isEditing) "Edit Product" else "Add Product",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("add_product_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
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
                Box(modifier = Modifier.padding(16.dp)) {
                    PrimaryButton(
                        text = if (isEditing) "Save Changes" else "Add Product",
                        icon = if (isEditing) Icons.Filled.Save else Icons.Filled.AddCircle,
                        onClick = {
                            var hasError = false
                            if (barcode.isBlank()) {
                                barcodeError = "Barcode is required"
                                hasError = true
                            } else {
                                barcodeError = null
                            }

                            if (name.isBlank()) {
                                nameError = "Product name is required"
                                hasError = true
                            } else {
                                nameError = null
                            }

                            val price = priceStr.toDoubleOrNull()
                            if (price == null || price <= 0.0) {
                                priceError = "Enter a valid positive price"
                                hasError = true
                            } else {
                                priceError = null
                            }

                            if (!hasError) {
                                val stock = stockStr.toIntOrNull() ?: 0
                                if (isEditing) {
                                    val updated = existingProduct!!.copy(
                                        name = name.trim(),
                                        price = price!!,
                                        stock = stock
                                    )
                                    productViewModel.updateProduct(updated) {
                                        onNavigateBack()
                                    }
                                } else {
                                    productViewModel.addProduct(
                                        name = name.trim(),
                                        barcode = barcode.trim(),
                                        price = price!!,
                                        stock = stock
                                    ) {
                                        onNavigateBack()
                                    }
                                }
                            }
                        },
                        testTag = "submit_product_button"
                    )
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
                .padding(20.dp)
        ) {
            if (isEditing) {
                // Immutable barcode badge for edit mode
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = PrimaryPurple.copy(alpha = 0.06f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryPurple.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.QrCodeScanner,
                            contentDescription = null,
                            tint = PrimaryPurple,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "BARCODE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = PrimaryPurple.copy(alpha = 0.8f),
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = barcode,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextPrimary
                                )
                            )
                        }
                    }
                }
            } else {
                // Barcode input with scan button
                InputLabel(text = "Barcode", subtitle = "Scan with camera or type")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppTextField(
                        value = barcode,
                        onValueChange = {
                            barcode = it
                            if (barcodeError != null) barcodeError = null
                        },
                        placeholder = "Scan or enter barcode number",
                        isError = barcodeError != null,
                        errorMessage = barcodeError,
                        modifier = Modifier.weight(1f),
                        testTag = "product_barcode_input"
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = PrimaryPurple.copy(alpha = 0.1f),
                        modifier = Modifier.size(56.dp)
                    ) {
                        IconButton(
                            onClick = onOpenScanner,
                            modifier = Modifier.testTag("scan_barcode_for_add_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.QrCodeScanner,
                                contentDescription = "Scan Barcode",
                                tint = PrimaryPurple,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Product Name
            InputLabel(text = "Product Name")
            AppTextField(
                value = name,
                onValueChange = {
                    name = it
                    if (nameError != null) nameError = null
                },
                placeholder = "e.g. Basmati Rice 1kg",
                isError = nameError != null,
                errorMessage = nameError,
                testTag = "product_name_input"
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Price Field
            InputLabel(text = "Price (INR)")
            AppTextField(
                value = priceStr,
                onValueChange = {
                    priceStr = it
                    if (priceError != null) priceError = null
                },
                placeholder = "0.00",
                prefix = "₹ ",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = priceError != null,
                errorMessage = priceError,
                testTag = "product_price_input"
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Stock Field
            InputLabel(text = "Stock Quantity (Optional)")
            AppTextField(
                value = stockStr,
                onValueChange = { stockStr = it },
                placeholder = "e.g. 50",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                testTag = "product_stock_input"
            )

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
