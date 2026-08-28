package com.example.billing_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.billing_app.data.db.AppDatabase
import com.example.billing_app.data.repository.PrinterRepository
import com.example.billing_app.data.repository.ProductRepository
import com.example.billing_app.data.repository.ShopRepository
import com.example.billing_app.ui.navigation.AppNavigation
import com.example.billing_app.ui.theme.BillingAppTheme
import com.example.billing_app.ui.viewmodel.AppViewModelFactory
import com.example.billing_app.ui.viewmodel.BillingViewModel
import com.example.billing_app.ui.viewmodel.PrinterViewModel
import com.example.billing_app.ui.viewmodel.ProductViewModel
import com.example.billing_app.ui.viewmodel.ShopViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var database: AppDatabase
    private lateinit var productRepository: ProductRepository
    private lateinit var shopRepository: ShopRepository
    private lateinit var printerRepository: PrinterRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "billing_app.db"
        ).fallbackToDestructiveMigration().build()

        productRepository = ProductRepository(database.productDao())
        shopRepository = ShopRepository(database.shopDao())
        printerRepository = PrinterRepository(applicationContext)

        lifecycleScope.launch {
            productRepository.seedInitialProductsIfEmpty()
            shopRepository.seedDefaultShopIfEmpty()
        }

        val viewModelFactory = AppViewModelFactory(
            productRepository = productRepository,
            shopRepository = shopRepository,
            printerRepository = printerRepository
        )

        val billingViewModel: BillingViewModel by viewModels { viewModelFactory }
        val productViewModel: ProductViewModel by viewModels { viewModelFactory }
        val shopViewModel: ShopViewModel by viewModels { viewModelFactory }
        val printerViewModel: PrinterViewModel by viewModels { viewModelFactory }

        setContent {
            BillingAppTheme {
                AppNavigation(
                    billingViewModel = billingViewModel,
                    productViewModel = productViewModel,
                    shopViewModel = shopViewModel,
                    printerViewModel = printerViewModel
                )
            }
        }
    }
}
