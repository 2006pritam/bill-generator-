package com.example.billing_app.data.db

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import com.example.billing_app.domain.model.Product
import com.example.billing_app.domain.model.Shop
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "barcode") val barcode: String,
    @ColumnInfo(name = "price") val price: Double,
    @ColumnInfo(name = "stock") val stock: Int = 0
) {
    fun toDomain(): Product = Product(
        id = id,
        name = name,
        barcode = barcode,
        price = price,
        stock = stock
    )

    companion object {
        fun fromDomain(product: Product): ProductEntity = ProductEntity(
            id = product.id,
            name = product.name,
            barcode = product.barcode,
            price = product.price,
            stock = product.stock
        )
    }
}

@Entity(tableName = "shop_details")
data class ShopEntity(
    @PrimaryKey val id: Int = 1,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "address_line_1") val addressLine1: String,
    @ColumnInfo(name = "address_line_2") val addressLine2: String,
    @ColumnInfo(name = "phone_number") val phoneNumber: String,
    @ColumnInfo(name = "upi_id") val upiId: String,
    @ColumnInfo(name = "footer_text") val footerText: String
) {
    fun toDomain(): Shop = Shop(
        id = id,
        name = name,
        addressLine1 = addressLine1,
        addressLine2 = addressLine2,
        phoneNumber = phoneNumber,
        upiId = upiId,
        footerText = footerText
    )

    companion object {
        fun fromDomain(shop: Shop): ShopEntity = ShopEntity(
            id = shop.id,
            name = shop.name,
            addressLine1 = shop.addressLine1,
            addressLine2 = shop.addressLine2,
            phoneNumber = shop.phoneNumber,
            upiId = shop.upiId,
            footerText = shop.footerText
        )
    }
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun getProductByBarcode(barcode: String): ProductEntity?

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: String): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Delete
    suspend fun deleteProduct(product: ProductEntity)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteProductById(id: String)

    @Query("DELETE FROM products")
    suspend fun deleteAllProducts()

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertInitialProducts(products: List<ProductEntity>)
}

@Dao
interface ShopDao {
    @Query("SELECT * FROM shop_details WHERE id = 1 LIMIT 1")
    fun getShopDetails(): Flow<ShopEntity?>

    @Query("SELECT * FROM shop_details WHERE id = 1 LIMIT 1")
    suspend fun getShopDetailsOnce(): ShopEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveShopDetails(shop: ShopEntity)
}

@Database(
    entities = [ProductEntity::class, ShopEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun shopDao(): ShopDao
}
