package com.example.billing_app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.billing_app.domain.model.Product
import com.example.billing_app.ui.screens.AddEditProductScreen
import com.example.billing_app.ui.screens.CheckoutScreen
import com.example.billing_app.ui.screens.HomeScreen
import com.example.billing_app.ui.screens.ProductListScreen
import com.example.billing_app.ui.screens.ScannerScreen
import com.example.billing_app.ui.screens.SettingsScreen
import com.example.billing_app.ui.screens.ShopDetailsScreen
import com.example.billing_app.ui.viewmodel.BillingViewModel
import com.example.billing_app.ui.viewmodel.PrinterViewModel
import com.example.billing_app.ui.viewmodel.ProductViewModel
import com.example.billing_app.ui.viewmodel.ShopViewModel

object Destinations {
    const val HOME = "home"
    const val CHECKOUT = "checkout"
    const val PRODUCT_LIST = "product_list"
    const val ADD_PRODUCT = "add_product"
    const val ADD_PRODUCT_ROUTE = "add_product?barcode={barcode}"
    const val EDIT_PRODUCT = "edit_product/{productId}"
    const val SHOP_DETAILS = "shop_details"
    const val SETTINGS = "settings"
    const val SCANNER_FOR_SEARCH = "scanner_search"
    const val SCANNER_FOR_ADD = "scanner_add"

    fun addProduct(barcode: String? = null): String {
        return if (!barcode.isNullOrBlank()) "add_product?barcode=$barcode" else "add_product"
    }
}

@Composable
fun AppNavigation(
    billingViewModel: BillingViewModel,
    productViewModel: ProductViewModel,
    shopViewModel: ShopViewModel,
    printerViewModel: PrinterViewModel,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Destinations.HOME
    ) {
        composable(Destinations.HOME) {
            HomeScreen(
                billingViewModel = billingViewModel,
                onNavigateToCheckout = {
                    navController.navigate(Destinations.CHECKOUT)
                },
                onNavigateToSettings = {
                    navController.navigate(Destinations.SETTINGS)
                },
                onNavigateToAddProductWithBarcode = { barcode ->
                    navController.navigate(Destinations.addProduct(barcode))
                }
            )
        }

        composable(Destinations.CHECKOUT) {
            CheckoutScreen(
                billingViewModel = billingViewModel,
                shopViewModel = shopViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Destinations.SETTINGS) {
            SettingsScreen(
                shopViewModel = shopViewModel,
                printerViewModel = printerViewModel,
                onNavigateToProducts = {
                    navController.navigate(Destinations.PRODUCT_LIST)
                },
                onNavigateToShopDetails = {
                    navController.navigate(Destinations.SHOP_DETAILS)
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Destinations.PRODUCT_LIST) {
            ProductListScreen(
                productViewModel = productViewModel,
                onNavigateToAddProduct = { barcode ->
                    navController.navigate(Destinations.addProduct(barcode))
                },
                onNavigateToEditProduct = { product ->
                    navController.navigate("edit_product/${product.id}")
                },
                onOpenScanner = {
                    navController.navigate(Destinations.SCANNER_FOR_SEARCH)
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Destinations.ADD_PRODUCT_ROUTE,
            arguments = listOf(
                navArgument("barcode") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val argBarcode = backStackEntry.arguments?.getString("barcode")
            val liveScannedBarcode by backStackEntry.savedStateHandle
                .getStateFlow<String?>("scanned_barcode", null)
                .collectAsStateWithLifecycle()

            val effectiveBarcode = liveScannedBarcode ?: argBarcode

            AddEditProductScreen(
                productViewModel = productViewModel,
                existingProduct = null,
                scannedBarcode = effectiveBarcode,
                onOpenScanner = {
                    navController.navigate(Destinations.SCANNER_FOR_ADD)
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Destinations.EDIT_PRODUCT,
            arguments = listOf(navArgument("productId") { type = NavType.StringType })
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: ""
            val productState = productViewModel.uiState.value
            val existingProduct = remember(productId, productState.products) {
                productState.products.find { it.id == productId }
            }

            AddEditProductScreen(
                productViewModel = productViewModel,
                existingProduct = existingProduct,
                scannedBarcode = null,
                onOpenScanner = {},
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Destinations.SHOP_DETAILS) {
            ShopDetailsScreen(
                shopViewModel = shopViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Destinations.SCANNER_FOR_SEARCH) {
            ScannerScreen(
                onBarcodeScanned = { barcode ->
                    productViewModel.setSearchQuery(barcode)
                    navController.popBackStack()
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Destinations.SCANNER_FOR_ADD) {
            ScannerScreen(
                onBarcodeScanned = { barcode ->
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("scanned_barcode", barcode)
                    navController.popBackStack()
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

