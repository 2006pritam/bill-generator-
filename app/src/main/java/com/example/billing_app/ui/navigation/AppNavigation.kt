package com.example.billing_app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
    const val EDIT_PRODUCT = "edit_product/{productId}"
    const val SHOP_DETAILS = "shop_details"
    const val SETTINGS = "settings"
    const val SCANNER_FOR_SEARCH = "scanner_search"
    const val SCANNER_FOR_ADD = "scanner_add"
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
                onNavigateToAddProduct = {
                    navController.navigate(Destinations.ADD_PRODUCT)
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

        composable(Destinations.ADD_PRODUCT) { backStackEntry ->
            val scannedBarcode = backStackEntry.savedStateHandle.get<String>("scanned_barcode")
            AddEditProductScreen(
                productViewModel = productViewModel,
                existingProduct = null,
                scannedBarcode = scannedBarcode,
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
