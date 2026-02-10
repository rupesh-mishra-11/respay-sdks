package com.amenitypay.sdk

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import com.amenitypay.sdk.auth.SessionManager
import com.amenitypay.sdk.callback.AmenityPayCallback
import com.amenitypay.sdk.callback.handleResult
import com.amenitypay.sdk.config.SDKConfig
import com.amenitypay.sdk.models.AmenityBookingRequest
import com.amenitypay.sdk.models.AmenityPayResult
import com.amenitypay.sdk.models.AuthCredentials
import com.amenitypay.sdk.network.NetworkClient
import com.amenitypay.sdk.repository.AuthRepository
import com.amenitypay.sdk.repository.PaymentRepository
import com.amenitypay.sdk.ui.payment.PaymentMethodsActivity

/**
 * Main entry point for AmenityPay SDK
 *
 * Usage:
 * ```kotlin
 * // 1. Initialize the SDK (typically in Application.onCreate())
 * val config = SDKConfig.builder()
 *     .baseUrl("https://api.example.com/v1/")
 *     .subscriptionKey("your-subscription-key")
 *     .environment(SDKConfig.Environment.DEVELOPMENT)
 *     .useMockData(true) // Use mock data for testing
 *     .build()
 *
 * AmenityPaySDK.initialize(context, config)
 *
 * // 2. Set authentication credentials (from your app's login)
 * AmenityPaySDK.setAuthCredentials(
 *     clientId = 12345,
 *     propertyId = 67890,
 *     customerId = 11111,
 *     leaseId = 22222
 * )
 *
 * // 3. Launch amenity payment flow
 * val bookingRequest = AmenityBookingRequest(
 *     amenityId = 100,
 *     amenityName = "Pool Access",
 *     amount = 50.00,
 *     startDatetime = "2024-01-15T10:00:00Z",
 *     endDatetime = "2024-01-15T12:00:00Z"
 * )
 *
 * AmenityPaySDK.launchPayment(activity, bookingRequest, REQUEST_CODE)
 *
 * // 4. Handle result in onActivityResult
 * override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
 *     if (requestCode == REQUEST_CODE) {
 *         AmenityPaySDK.handleResult(data, object : AmenityPayCallback {
 *             override fun onPaymentSuccess(result: AmenityPayResult.Success) {
 *                 // Handle success
 *             }
 *             override fun onPaymentFailure(result: AmenityPayResult.Failure) {
 *                 // Handle failure
 *             }
 *             override fun onPaymentCancelled() {
 *                 // Handle cancellation
 *             }
 *         })
 *     }
 * }
 * ```
 */
object AmenityPaySDK {
    
    private var isInitialized = false
    private var config: SDKConfig? = null
    private var sessionManager: SessionManager? = null
    private var authRepository: AuthRepository? = null
    private var paymentRepository: PaymentRepository? = null
    
    /**
     * SDK Version
     */
    const val VERSION = "1.0.0"
    
    /**
     * Initialize the SDK with configuration
     *
     * @param context Application context
     * @param sdkConfig SDK configuration
     */
    fun initialize(context: Context, sdkConfig: SDKConfig) {
        if (isInitialized) {
            return
        }
        
        config = sdkConfig
        sessionManager = SessionManager(context.applicationContext)
        authRepository = AuthRepository(sessionManager!!)
        paymentRepository = PaymentRepository()
        
        NetworkClient.initialize(sdkConfig, sessionManager!!)
        
        isInitialized = true
    }
    
    /**
     * Check if SDK is initialized
     */
    fun isInitialized(): Boolean = isInitialized
    
    /**
     * Set authentication credentials from host app
     * Call this after your app's login to pass the customer info to the SDK
     *
     * @param clientId The client/company ID
     * @param propertyId The property ID
     * @param customerId The customer/resident ID
     * @param leaseId The lease ID
     */
    fun setAuthCredentials(
        clientId: Long,
        propertyId: Long,
        customerId: Long,
        leaseId: Long
    ) {
        ensureInitialized()
        
        val credentials = AuthCredentials(
            clientId = clientId,
            propertyId = propertyId,
            customerId = customerId,
            leaseId = leaseId
        )
        sessionManager?.saveCredentials(credentials)
    }
    
    /**
     * Get current auth credentials
     */
    fun getAuthCredentials(): AuthCredentials? {
        ensureInitialized()
        return sessionManager?.getCredentials()
    }
    
    /**
     * Launch the amenity payment flow
     * Requires credentials to be set via setAuthCredentials()
     *
     * @param activity The activity to launch from
     * @param bookingRequest The amenity booking details
     * @param requestCode Request code for onActivityResult
     */
    fun launchPayment(
        activity: Activity,
        bookingRequest: AmenityBookingRequest,
        requestCode: Int
    ) {
        ensureInitialized()
        
        // Check if credentials are set
        if (!hasValidCredentials()) {
            throw IllegalStateException(
                "Auth credentials not set. Call AmenityPaySDK.setAuthCredentials() first."
            )
        }
        
        // Go directly to payment methods (no login needed - credentials already set)
        val intent = Intent(activity, PaymentMethodsActivity::class.java).apply {
            putExtra(PaymentMethodsActivity.EXTRA_BOOKING_REQUEST, bookingRequest)
        }
        activity.startActivityForResult(intent, requestCode)
    }
    
    /**
     * Launch the amenity payment flow using ActivityResultLauncher
     *
     * @param context Context for creating intent
     * @param launcher ActivityResultLauncher for handling the result
     * @param bookingRequest The amenity booking details
     */
    fun launchPayment(
        context: Context,
        launcher: ActivityResultLauncher<Intent>,
        bookingRequest: AmenityBookingRequest
    ) {
        ensureInitialized()
        
        if (!hasValidCredentials()) {
            throw IllegalStateException(
                "Auth credentials not set. Call AmenityPaySDK.setAuthCredentials() first."
            )
        }
        
        val intent = Intent(context, PaymentMethodsActivity::class.java).apply {
            putExtra(PaymentMethodsActivity.EXTRA_BOOKING_REQUEST, bookingRequest)
        }
        launcher.launch(intent)
    }
    
    /**
     * Handle the result from the SDK
     *
     * @param data Intent data from onActivityResult
     * @param callback Callback to receive the result
     */
    fun handleResult(data: Intent?, callback: AmenityPayCallback) {
        @Suppress("DEPRECATION")
        val result = data?.getSerializableExtra(SDKConstants.EXTRA_RESULT) as? AmenityPayResult
            ?: AmenityPayResult.Cancelled
        
        callback.handleResult(result)
    }
    
    /**
     * Parse the result from Intent
     *
     * @param data Intent data from onActivityResult
     * @return AmenityPayResult or Cancelled if not available
     */
    fun parseResult(data: Intent?): AmenityPayResult {
        @Suppress("DEPRECATION")
        return data?.getSerializableExtra(SDKConstants.EXTRA_RESULT) as? AmenityPayResult
            ?: AmenityPayResult.Cancelled
    }
    
    /**
     * Check if valid credentials are set
     */
    fun hasValidCredentials(): Boolean {
        ensureInitialized()
        return sessionManager?.hasValidCredentials() ?: false
    }
    
    /**
     * Clear SDK session (clears credentials)
     */
    fun clearSession() {
        sessionManager?.clearSession()
    }
    
    /**
     * Reset SDK (clear all data and reinitialize required)
     */
    fun reset() {
        clearSession()
        NetworkClient.clear()
        
        config = null
        sessionManager = null
        authRepository = null
        paymentRepository = null
        isInitialized = false
    }
    
    /**
     * Get current SDK configuration
     */
    fun getConfig(): SDKConfig {
        ensureInitialized()
        return config!!
    }
    
    /**
     * Get subscription key
     */
    fun getSubscriptionKey(): String {
        ensureInitialized()
        return config!!.subscriptionKey
    }
    
    // Internal methods for accessing repositories
    internal fun getAuthRepository(): AuthRepository {
        ensureInitialized()
        return authRepository!!
    }
    
    internal fun getPaymentRepository(): PaymentRepository {
        ensureInitialized()
        return paymentRepository!!
    }
    
    internal fun getSessionManager(): SessionManager {
        ensureInitialized()
        return sessionManager!!
    }
    
    private fun ensureInitialized() {
        if (!isInitialized) {
            throw IllegalStateException(
                "AmenityPaySDK is not initialized. Call AmenityPaySDK.initialize() first."
            )
        }
    }
}
