package com.example.billing_app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Phone
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.billing_app.ui.components.AppTextField
import com.example.billing_app.ui.components.InputLabel
import com.example.billing_app.ui.components.PrimaryButton
import com.example.billing_app.ui.theme.PrimaryPurple
import com.example.billing_app.ui.viewmodel.ShopViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopDetailsScreen(
    shopViewModel: ShopViewModel,
    onNavigateBack: () -> Unit
) {
    val currentShop by shopViewModel.shop.collectAsStateWithLifecycle()
    val savedEvent by shopViewModel.savedEvent.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var name by remember(currentShop) { mutableStateOf(currentShop.name) }
    var address1 by remember(currentShop) { mutableStateOf(currentShop.addressLine1) }
    var address2 by remember(currentShop) { mutableStateOf(currentShop.addressLine2) }
    var phone by remember(currentShop) { mutableStateOf(currentShop.phoneNumber) }
    var upi by remember(currentShop) { mutableStateOf(currentShop.upiId) }
    var footer by remember(currentShop) { mutableStateOf(currentShop.footerText) }

    var nameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(savedEvent) {
        if (savedEvent) {
            snackbarHostState.showSnackbar("Shop details saved successfully!")
            shopViewModel.resetSavedEvent()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Shop Details",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("shop_details_back_button")
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
                        text = "Save Shop Details",
                        icon = Icons.Filled.Save,
                        onClick = {
                            var hasError = false
                            if (name.isBlank()) {
                                nameError = "Shop name cannot be empty"
                                hasError = true
                            } else {
                                nameError = null
                            }

                            if (phone.isBlank()) {
                                phoneError = "Phone number is required"
                                hasError = true
                            } else {
                                phoneError = null
                            }

                            if (!hasError) {
                                shopViewModel.updateShop(
                                    name = name,
                                    address1 = address1,
                                    address2 = address2,
                                    phone = phone,
                                    upi = upi,
                                    footer = footer
                                ) {
                                    onNavigateBack()
                                }
                            }
                        },
                        testTag = "save_shop_details_button"
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
            // Shop Name
            InputLabel(text = "Shop / Store Name")
            AppTextField(
                value = name,
                onValueChange = {
                    name = it
                    if (nameError != null) nameError = null
                },
                placeholder = "e.g. Elite Groceries",
                leadingIcon = Icons.Filled.Business,
                isError = nameError != null,
                errorMessage = nameError,
                testTag = "shop_name_input"
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Address Line 1
            InputLabel(text = "Address Line 1")
            AppTextField(
                value = address1,
                onValueChange = { address1 = it },
                placeholder = "e.g. 123 Market Street",
                leadingIcon = Icons.Filled.LocationOn,
                testTag = "shop_address1_input"
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Address Line 2
            InputLabel(text = "Address Line 2 (City, Postal Code)")
            AppTextField(
                value = address2,
                onValueChange = { address2 = it },
                placeholder = "e.g. Central City, 560001",
                leadingIcon = Icons.Filled.LocationOn,
                testTag = "shop_address2_input"
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Phone Number
            InputLabel(text = "Phone Number")
            AppTextField(
                value = phone,
                onValueChange = {
                    phone = it
                    if (phoneError != null) phoneError = null
                },
                placeholder = "e.g. +91 9876543210",
                leadingIcon = Icons.Filled.Phone,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                isError = phoneError != null,
                errorMessage = phoneError,
                testTag = "shop_phone_input"
            )

            Spacer(modifier = Modifier.height(20.dp))

            // UPI ID
            InputLabel(text = "UPI ID for Digital Payments", subtitle = "Used for QR Code")
            AppTextField(
                value = upi,
                onValueChange = { upi = it },
                placeholder = "e.g. storename@upi or 9876543210@paytm",
                leadingIcon = Icons.Filled.Payment,
                testTag = "shop_upi_input"
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Footer Text
            InputLabel(text = "Receipt Footer Message", subtitle = "Printed at bottom of bill")
            AppTextField(
                value = footer,
                onValueChange = { footer = it },
                placeholder = "e.g. Thank you for shopping with us! Visit again.",
                leadingIcon = Icons.Filled.ChatBubbleOutline,
                singleLine = false,
                maxLines = 3,
                testTag = "shop_footer_input"
            )

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
