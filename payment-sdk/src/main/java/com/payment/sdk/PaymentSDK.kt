package com.payment.sdk

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.payment.sdk.callback.PaymentCallback
import com.payment.sdk.config.PaymentConfig
import com.payment.sdk.models.PaymentRequest
import com.payment.sdk.models.PaymentResult
import com.payment.sdk.network.NetworkClient
import com.payment.sdk.repository.PaymentRepository
import com.payment.sdk.ui.payment.MakePaymentActivity

/**
 * Main entry point for the Payment SDK.
 * 
 * Usage:
 * 1. Initialize: PaymentSDK.initialize(context, config)
 * 2. Set customer: PaymentSDK.setCustomer(customerId, authToken)
 * 3. Make payment: PaymentSDK.makePayment(activity, amount, callback)
 */
object PaymentSDK {
    
    private var isInitialized = false
    private var config: PaymentConfig? = null
    private var appContext: Context? = null
    private var customerId: Long? = null
    private var authToken: String? = null
    private var paymentCallback: PaymentCallback? = null
    private var paymentRepository: PaymentRepository? = null
    
    /**
     * Initialize the SDK. Must be called before any other method.
     */
    fun initialize(context: Context, config: PaymentConfig) {
        if (isInitialized) return
        
        this.appContext = context.applicationContext
        this.config = config
        
        NetworkClient.initialize(config)
        this.paymentRepository = PaymentRepository()
        
        isInitialized = true
    }
    
    /**
     * Set customer credentials for API authentication.
     */
    fun setCustomer(customerId: Long, authToken: String) {
        ensureInitialized()
        this.customerId = customerId
        this.authToken = authToken
    }
    
    /**
     * Launch the payment screen.
     * 
     * @param activity The calling activity
     * @param paymentRequest Payment details (amount, currency, description)
     * @param callback Callback for payment result
     */
    fun makePayment(
        activity: Activity,
        paymentRequest: PaymentRequest,
        callback: PaymentCallback
    ) {
        ensureInitialized()
        ensureCustomerSet()
        
        this.paymentCallback = callback
        
        val intent = Intent(activity, MakePaymentActivity::class.java).apply {
            putExtra(EXTRA_PAYMENT_REQUEST, paymentRequest)
        }
        
        @Suppress("DEPRECATION")
        activity.startActivityForResult(intent, REQUEST_CODE_PAYMENT)
    }
    
    /**
     * Convenience method to make a simple payment with just amount.
     */
    fun makePayment(
        activity: Activity,
        amount: Double,
        callback: PaymentCallback
    ) {
        makePayment(
            activity = activity,
            paymentRequest = PaymentRequest(
                amount = amount,
                currency = "USD",
                description = null
            ),
            callback = callback
        )
    }
    
    /**
     * Clear customer session.
     */
    fun clearCustomer() {
        customerId = null
        authToken = null
    }
    
    // Internal accessors
    internal fun getConfig(): PaymentConfig {
        ensureInitialized()
        return config!!
    }
    
    internal fun getCustomerId(): Long {
        ensureCustomerSet()
        return customerId!!
    }
    
    internal fun getAuthToken(): String {
        ensureCustomerSet()
        return authToken!!
    }
    
    internal fun getPaymentRepository(): PaymentRepository {
        ensureInitialized()
        return paymentRepository!!
    }
    
    internal fun getCallback(): PaymentCallback? = paymentCallback
    
    internal fun notifyResult(result: PaymentResult) {
        paymentCallback?.onPaymentResult(result)
    }
    
    private fun ensureInitialized() {
        if (!isInitialized) {
            throw IllegalStateException("PaymentSDK not initialized. Call initialize() first.")
        }
    }
    
    private fun ensureCustomerSet() {
        if (customerId == null || authToken == null) {
            throw IllegalStateException("Customer not set. Call setCustomer() first.")
        }
    }
    
    const val REQUEST_CODE_PAYMENT = 6001
    const val EXTRA_PAYMENT_REQUEST = "payment_request"
    const val EXTRA_PAYMENT_RESULT = "payment_result"
}
