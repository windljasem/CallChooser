package com.callchooser.app

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * BillingManager - управління покупками через Google Play Billing
 * 
 * Функціонал:
 * - Підключення до Google Play Billing
 * - Перевірка існуючих покупок (isPremium)
 * - Ініціювання покупки Premium (2.99€)
 * - Restore purchases
 */
class BillingManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    
    companion object {
        private const val TAG = "BillingManager"
        
        // Product ID (створити в Play Console!)
        const val PREMIUM_PRODUCT_ID = "callchooser_premium"
    }
    
    // Billing Client
    private lateinit var billingClient: BillingClient
    
    // Callbacks
    private var onPremiumStatusChanged: ((Boolean) -> Unit)? = null
    private var onPurchaseSuccess: (() -> Unit)? = null
    private var onPurchaseError: ((String) -> Unit)? = null
    
    // State
    private var isConnected = false
    
    /**
     * Ініціалізація Billing Client
     */
    fun initialize(
        onPremiumStatusChanged: (Boolean) -> Unit,
        onConnectionReady: () -> Unit = {}
    ) {
        this.onPremiumStatusChanged = onPremiumStatusChanged
        
        billingClient = BillingClient.newBuilder(context)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases()
            .build()
        
        // Підключення до Google Play
        startConnection(onConnectionReady)
    }
    
    /**
     * Підключення до Google Play Billing
     */
    private fun startConnection(onReady: () -> Unit) {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    isConnected = true
                    Log.d(TAG, "✅ Billing connected")
                    
                    // Перевіряємо існуючі покупки
                    queryPurchases()
                    
                    onReady()
                } else {
                    Log.e(TAG, "❌ Billing connection failed: ${billingResult.debugMessage}")
                }
            }
            
            override fun onBillingServiceDisconnected() {
                isConnected = false
                Log.w(TAG, "⚠️ Billing disconnected, will retry")
                
                // Retry connection після 3 секунд
                scope.launch {
                    kotlinx.coroutines.delay(3000)
                    if (!isConnected) {
                        startConnection(onReady)
                    }
                }
            }
        })
    }
    
    /**
     * Listener для оновлень покупок
     */
    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (purchases != null && purchases.isNotEmpty()) {
                    Log.d(TAG, "✅ Purchase successful!")
                    
                    // Обробляємо кожну покупку
                    for (purchase in purchases) {
                        handlePurchase(purchase)
                    }
                }
            }
            
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.d(TAG, "ℹ️ User canceled purchase")
                onPurchaseError?.invoke("Purchase canceled")
            }
            
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                Log.d(TAG, "ℹ️ Item already owned")
                // Перевіряємо покупки знову
                queryPurchases()
                onPurchaseSuccess?.invoke()
            }
            
            else -> {
                Log.e(TAG, "❌ Purchase error: ${billingResult.debugMessage}")
                onPurchaseError?.invoke(billingResult.debugMessage)
            }
        }
    }
    
    /**
     * Обробка покупки
     */
    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            
            // Якщо покупка не acknowledged - робимо acknowledge
            if (!purchase.isAcknowledged) {
                acknowledgePurchase(purchase)
            } else {
                // Покупка вже acknowledged
                savePremiumStatus(true)
                onPremiumStatusChanged?.invoke(true)
                onPurchaseSuccess?.invoke()
            }
        }
    }
    
    /**
     * Acknowledge покупки (підтвердження)
     */
    private fun acknowledgePurchase(purchase: Purchase) {
        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        
        scope.launch {
            withContext(Dispatchers.IO) {
                billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        Log.d(TAG, "✅ Purchase acknowledged")
                        savePremiumStatus(true)
                        onPremiumStatusChanged?.invoke(true)
                        onPurchaseSuccess?.invoke()
                    } else {
                        Log.e(TAG, "❌ Acknowledge failed: ${billingResult.debugMessage}")
                    }
                }
            }
        }
    }
    
    /**
     * Перевірка існуючих покупок
     */
    fun queryPurchases() {
        if (!isConnected) {
            Log.w(TAG, "⚠️ Not connected, cannot query purchases")
            return
        }
        
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        
        scope.launch {
            withContext(Dispatchers.IO) {
                billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        
                        // Шукаємо premium покупку
                        val hasPremium = purchases.any { purchase ->
                            purchase.products.contains(PREMIUM_PRODUCT_ID) &&
                            purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                        }
                        
                        Log.d(TAG, "Premium status: $hasPremium (found ${purchases.size} purchases)")
                        
                        savePremiumStatus(hasPremium)
                        onPremiumStatusChanged?.invoke(hasPremium)
                        
                    } else {
                        Log.e(TAG, "❌ Query purchases failed: ${billingResult.debugMessage}")
                    }
                }
            }
        }
    }
    
    /**
     * Ініціювання покупки Premium
     */
    fun launchPurchaseFlow(
        activity: Activity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        this.onPurchaseSuccess = onSuccess
        this.onPurchaseError = onError
        
        if (!isConnected) {
            onError("Billing service not connected")
            return
        }
        
        // Query product details
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PREMIUM_PRODUCT_ID)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )
        
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()
        
        scope.launch {
            withContext(Dispatchers.IO) {
                billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
                    
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
                        
                        val productDetails = productDetailsList[0]
                        
                        // Launch purchase flow
                        val productDetailsParamsList = listOf(
                            BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(productDetails)
                                .build()
                        )
                        
                        val billingFlowParams = BillingFlowParams.newBuilder()
                            .setProductDetailsParamsList(productDetailsParamsList)
                            .build()
                        
                        billingClient.launchBillingFlow(activity, billingFlowParams)
                        
                    } else {
                        Log.e(TAG, "❌ Product not found or error: ${billingResult.debugMessage}")
                        onError("Product not available")
                    }
                }
            }
        }
    }
    
    /**
     * Restore purchases (для користувачів які переустановили апку)
     */
    fun restorePurchases(
        onSuccess: (Boolean) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!isConnected) {
            onError("Billing service not connected")
            return
        }
        
        Log.d(TAG, "Restoring purchases...")
        
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        
        scope.launch {
            withContext(Dispatchers.IO) {
                billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        
                        val premiumPurchase = purchases.find { purchase ->
                            purchase.products.contains(PREMIUM_PRODUCT_ID) &&
                            purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                        }
                        
                        if (premiumPurchase != null) {
                            Log.d(TAG, "✅ Premium purchase found, restoring...")
                            handlePurchase(premiumPurchase)
                            onSuccess(true)
                        } else {
                            Log.d(TAG, "ℹ️ No premium purchases found")
                            savePremiumStatus(false)
                            onSuccess(false)
                        }
                        
                    } else {
                        Log.e(TAG, "❌ Restore failed: ${billingResult.debugMessage}")
                        onError("Failed to restore purchases")
                    }
                }
            }
        }
    }
    
    /**
     * Збереження статусу Premium в SharedPreferences
     */
    private fun savePremiumStatus(isPremium: Boolean) {
        context.getSharedPreferences("callchooser", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("premium_unlocked", isPremium)
            .apply()
        
        Log.d(TAG, "Premium status saved: $isPremium")
    }
    
    /**
     * Перевірка чи є Premium (з SharedPreferences)
     */
    fun isPremiumUnlocked(): Boolean {
        val prefs = context.getSharedPreferences("callchooser", Context.MODE_PRIVATE)
        return prefs.getBoolean("premium_unlocked", false)
    }
    
    /**
     * Відключення від Billing (викликати в onDestroy)
     */
    fun disconnect() {
        if (::billingClient.isInitialized && isConnected) {
            billingClient.endConnection()
            isConnected = false
            Log.d(TAG, "✅ Billing disconnected")
        }
    }
}
