package com.applepay.sdk

import android.app.Activity
import android.content.Context
import com.applepay.sdk.auth.SessionTokenManager
import com.applepay.sdk.auth.TokenExecutionWrapper
import com.applepay.sdk.callback.ApplePayAvailabilityCallback
import com.applepay.sdk.callback.ApplePayCallback
import com.applepay.sdk.callback.ApplePayFinalizeCallback
import com.applepay.sdk.callback.ApplePayValidateCallback
import com.applepay.sdk.config.ApplePayConfig
import com.applepay.sdk.config.SessionData
import com.applepay.sdk.models.ApplePayInitRequest
import com.applepay.sdk.models.ApplePayValidateRequest
import com.applepay.sdk.models.ApplePayFinalizeRequest
import com.applepay.sdk.models.ApplePayWalletInfo
import com.applepay.sdk.network.NetworkClient
import com.applepay.sdk.repository.ApplePayRepository
import com.applepay.sdk.repository.RepositoryResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Main entry point for the Apple Pay SDK.
 * 
 * Usage:
 * ```kotlin
 * // 1. Initialize in Application.onCreate()
 * ApplePaySDK.initialize(
 *     context = applicationContext,
 *     config = ApplePayConfig.Builder()
 *         .baseUrl("https://api.example.com/v1/")
 *         .environment(ApplePayConfig.Environment.DEVELOPMENT)
 *         .enableLogging(false)
 *         .build(),
 *     clientType = "mobile-android"
 * )
 * 
 * // 2. Set session data after user login
 * ApplePaySDK.setSessionData(
 *     cid = 12345,
 *     propertyId = 67890,
 *     customerId = 11111L,
 *     leaseId = 22222L
 * )
 * 
 * // 3. Check availability (host app creates its own radio button UI)
 * ApplePaySDK.checkAvailability(
 *     amount = 99.99,
 *     callback = object : ApplePayAvailabilityCallback {
 *         override fun onAvailable(walletInfo: ApplePayWalletInfo) {
 *             // Show your app's Apple Pay radio button
 *             applePayRadioButton.visibility = View.VISIBLE
 *         }
 *         override fun onUnavailable(message: String) {
 *             // Hide your app's Apple Pay option
 *             applePayRadioButton.visibility = View.GONE
 *         }
 *         override fun onError(errorCode: String, message: String) {
 *             // Handle error
 *         }
 *     }
 * )
 * 
 * // 4. When radio button is clicked, start Apple Pay flow
 * applePayRadioButton.setOnClickListener {
 *     ApplePaySDK.startApplePay(
 *         activity = this,
 *         amount = 99.99,
 *         callback = object : ApplePayCallback {
 *             override fun onInitSuccess() {
 *                 // Token is stored internally, proceed with payment
 *                 // After getting payment data from Apple Pay, call validateApplePay()
 *             }
 *             override fun onFailure(errorCode: String, message: String) {
 *                 // Handle failure
 *             }
 *             override fun onCancelled() {
 *                 // Handle cancellation
 *             }
 *         }
 *     )
 * }
 * 
 * // 5. Validate payment data
 * ApplePaySDK.validateApplePay(
 *     paymentData = paymentDataString,
 *     callback = object : ApplePayValidateCallback {
 *         override fun onValidateSuccess(transactionId: String) {
 *             // Proceed to finalize
 *             ApplePaySDK.finalizeApplePay(transactionId, finalizeCallback)
 *         }
 *         override fun onValidateFailure(errorCode: String, message: String) {
 *             // Handle validation failure
 *         }
 *     }
 * )
 * 
 * // 6. Finalize payment
 * ApplePaySDK.finalizeApplePay(
 *     transactionId = transactionId,
 *     callback = object : ApplePayFinalizeCallback {
 *         override fun onFinalizeSuccess(
 *             transactionId: String,
 *             confirmationNumber: String?,
 *             amount: Double,
 *             currency: String
 *         ) {
 *             // Payment completed successfully
 *         }
 *         override fun onFinalizeFailure(errorCode: String, message: String) {
 *             // Handle finalization failure
 *         }
 *     }
 * )
 * ```
 */
object ApplePaySDK {
    
    private var isInitialized = false
    private var config: ApplePayConfig? = null
    private var appContext: Context? = null
    private var clientType: String? = null
    private var sessionData: SessionData? = null
    private var repository: ApplePayRepository? = null
    private var tokenWrapper: TokenExecutionWrapper? = null
    private var enableLogging: Boolean = false
    
    // Coroutine scope for SDK operations
    private val sdkScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    /**
     * SDK Version
     */
    const val VERSION = "1.0.0"
    
    /**
     * Initialize the SDK with configuration.
     * Must be called in Application.onCreate() before any other SDK methods.
     * 
     * @param context Application context
     * @param config SDK configuration (baseUrl, environment, etc.)
     * @param clientType Client type identifier (e.g., "mobile-android", "mobile-ios")
     *                   This is sent as x-client-type header to BFF
     */
    fun initialize(
        context: Context,
        config: ApplePayConfig,
        clientType: String
    ) {
        if (isInitialized) {
            if (isLoggingEnabled()) {
                android.util.Log.w("ApplePaySDK", "SDK already initialized, ignoring duplicate call")
            }
            return
        }
        
        require(clientType.isNotBlank()) { "Client type cannot be blank" }
        
        this.appContext = context.applicationContext
        this.config = config
        this.clientType = clientType
        this.enableLogging = config.enableLogging
        
        NetworkClient.initialize(context.applicationContext, config, clientType)
        this.repository = ApplePayRepository()
        
        // Initialize token execution wrapper (will be configured when session data is set)
        isInitialized = true
        
        if (enableLogging) {
            android.util.Log.d("ApplePaySDK", "SDK initialized successfully")
        }
    }
    
    /**
     * Check if SDK is initialized.
     */
    fun isInitialized(): Boolean = isInitialized
    
    /**
     * Set session data (customer, property, lease information).
     * Call this after user login in your app.
     * 
     * @param cid Client ID
     * @param propertyId Property ID
     * @param customerId Customer ID
     * @param leaseId Lease ID
     */
    fun setSessionData(
        cid: Int,
        propertyId: Int,
        customerId: Long,
        leaseId: Long
    ) {
        ensureInitialized()
        
        val data = SessionData(cid, propertyId, customerId, leaseId)
        require(data.validate()) { "All session data values must be greater than 0" }
        
        this.sessionData = data
        
        // Initialize token execution wrapper with session data
        repository?.let { repo ->
            val wrapper = TokenExecutionWrapper(repo) { amount ->
                val session = sessionData ?: throw IllegalStateException("Session data not set")
                ApplePayInitRequest(
                    data = ApplePayInitRequest.InitRequestData(
                        cid = session.cid,
                        propertyId = session.propertyId,
                        customerId = session.customerId,
                        leaseId = session.leaseId,
                        amount = amount
                    )
                )
            }
            this.tokenWrapper = wrapper
            repo.setTokenWrapper(wrapper)
        }
        
        if (enableLogging) {
            android.util.Log.d("ApplePaySDK", "Session data set for customer: $customerId")
        }
    }
    
    /**
     * Check Apple Pay availability by calling BFF /api/init endpoint.
     * 
     * This method:
     * - Calls POST /api/init with session data and amount
     * - Returns availability status via callback
     * - Host app should show/hide its Apple Pay radio button based on callback
     * 
     * Note: The radio button UI is the host app's responsibility. This SDK
     * only handles the Apple Pay flow once the button is clicked.
     * 
     * @param amount Payment amount
     * @param callback Callback for availability result
     */
    fun checkAvailability(
        amount: Double,
        callback: ApplePayAvailabilityCallback
    ) {
        ensureInitialized()
        ensureSessionData()
        require(amount > 0) { "Amount must be greater than 0" }
        
        val session = sessionData ?: return
        val repo = repository ?: return
        
        sdkScope.launch {
            val request = ApplePayInitRequest(
                data = ApplePayInitRequest.InitRequestData(
                    cid = session.cid,
                    propertyId = session.propertyId,
                    customerId = session.customerId,
                    leaseId = session.leaseId,
                    amount = amount
                )
            )
            
            when (val result = repo.initializeApplePay(request)) {
                is RepositoryResult.Success -> {
                    val response = result.data
                    
                    // Switch to main thread for UI callbacks
                    withContext(Dispatchers.Main) {
                        // Check if Apple Pay is available
                        if (response.isApplePayAvailable()) {
                            val walletInfo = ApplePayWalletInfo.fromResponse(response)
                            if (walletInfo != null) {
                                callback.onAvailable(walletInfo)
                            } else {
                                callback.onUnavailable("Apple Pay is not available.")
                            }
                        } else {
                            // Business rule: Apple Pay not enabled
                            val message = response.error?.message ?: "Apple Pay is not available."
                            callback.onUnavailable(message)
                        }
                    }
                }
                is RepositoryResult.Error -> {
                    // Switch to main thread for UI callbacks
                    withContext(Dispatchers.Main) {
                        callback.onError(result.code, result.message)
                    }
                }
            }
        }
    }
    
    /**
     * Start Apple Pay initialization flow.
     * 
     * This method should be called when the host app's Apple Pay radio button is clicked.
     * 
     * This method:
     * - Calls BFF /api/init endpoint
     * - Returns session token on success (stored in memory only)
     * - Host app should handle actual Apple Pay payment execution
     * 
     * @param activity Calling activity
     * @param amount Payment amount
     * @param callback Callback for initialization result
     */
    fun startApplePay(
        activity: Activity,
        amount: Double,
        callback: ApplePayCallback
    ) {
        ensureInitialized()
        ensureSessionData()
        require(amount > 0) { "Amount must be greater than 0" }
        
        val session = sessionData ?: return
        val repo = repository ?: return
        
        sdkScope.launch {
            val request = ApplePayInitRequest(
                data = ApplePayInitRequest.InitRequestData(
                    cid = session.cid,
                    propertyId = session.propertyId,
                    customerId = session.customerId,
                    leaseId = session.leaseId,
                    amount = amount
                )
            )
            
            when (val result = repo.initializeApplePay(request)) {
                is RepositoryResult.Success -> {
                    val response = result.data
                    
                    // Switch to main thread for UI callbacks
                    withContext(Dispatchers.Main) {
                        if (response.isApplePayAvailable()) {
                            // Store token internally (host app never sees it)
                            val token = response.token
                            if (token != null && token.isNotBlank()) {
                                SessionTokenManager.setToken(token)
                                
                                // Set amount in token wrapper for potential re-initialization
                                tokenWrapper?.setAmount(amount)
                                
                                callback.onInitSuccess()
                            } else {
                                callback.onFailure(
                                    "TOKEN_MISSING",
                                    "Init API did not return a token"
                                )
                            }
                        } else {
                            val errorCode = response.error?.code ?: "APPLE_PAY_DISABLED"
                            val message = response.error?.message ?: "Apple Pay is not available."
                            callback.onFailure(errorCode, message)
                        }
                    }
                }
                is RepositoryResult.Error -> {
                    // Switch to main thread for UI callbacks
                    withContext(Dispatchers.Main) {
                        callback.onFailure(result.code, result.message)
                    }
                }
            }
        }
    }
    
    /**
     * Validate Apple Pay payment data.
     * 
     * This is a protected endpoint that requires a session token.
     * Token management and retry logic is handled automatically by the SDK.
     * 
     * @param paymentData Payment data string from Apple Pay
     * @param callback Callback for validation result
     */
    fun validateApplePay(
        paymentData: String,
        callback: ApplePayValidateCallback
    ) {
        ensureInitialized()
        ensureSessionData()
        require(paymentData.isNotBlank()) { "Payment data cannot be blank" }
        
        val repo = repository ?: return
        
        sdkScope.launch {
            val request = ApplePayValidateRequest(
                data = ApplePayValidateRequest.ValidateRequestData(
                    paymentData = paymentData
                )
            )
            
            when (val result = repo.validateApplePay(request)) {
                is RepositoryResult.Success -> {
                    val response = result.data
                    val transactionId = response.data?.transactionId
                    
                    withContext(Dispatchers.Main) {
                        if (transactionId != null && transactionId.isNotBlank()) {
                            callback.onValidateSuccess(transactionId)
                        } else {
                            callback.onValidateFailure(
                                "INVALID_RESPONSE",
                                "Validation response missing transaction ID"
                            )
                        }
                    }
                }
                is RepositoryResult.Error -> {
                    withContext(Dispatchers.Main) {
                        callback.onValidateFailure(result.code, result.message)
                    }
                }
            }
        }
    }
    
    /**
     * Finalize Apple Pay payment.
     * 
     * This is a protected endpoint that requires a session token.
     * Token management and retry logic is handled automatically by the SDK.
     * Token is cleared after successful finalization or final failure.
     * 
     * @param transactionId Transaction ID from validation step
     * @param callback Callback for finalization result
     */
    fun finalizeApplePay(
        transactionId: String,
        callback: ApplePayFinalizeCallback
    ) {
        ensureInitialized()
        ensureSessionData()
        require(transactionId.isNotBlank()) { "Transaction ID cannot be blank" }
        
        val repo = repository ?: return
        
        sdkScope.launch {
            val request = ApplePayFinalizeRequest(
                data = ApplePayFinalizeRequest.FinalizeRequestData(
                    transactionId = transactionId
                )
            )
            
            when (val result = repo.finalizeApplePay(request)) {
                is RepositoryResult.Success -> {
                    val response = result.data
                    val finalizeData = response.data
                    
                    withContext(Dispatchers.Main) {
                        if (finalizeData != null && finalizeData.transactionId != null) {
                            // Clear token on successful payment completion
                            SessionTokenManager.clearToken()
                            
                            callback.onFinalizeSuccess(
                                transactionId = finalizeData.transactionId ?: transactionId,
                                confirmationNumber = finalizeData.confirmationNumber,
                                amount = finalizeData.amount ?: 0.0,
                                currency = finalizeData.currency ?: "USD"
                            )
                        } else {
                            // Clear token even on invalid response
                            SessionTokenManager.clearToken()
                            
                            callback.onFinalizeFailure(
                                "INVALID_RESPONSE",
                                "Finalization response missing required data"
                            )
                        }
                    }
                }
                is RepositoryResult.Error -> {
                    // Clear token on final failure
                    SessionTokenManager.clearToken()
                    
                    withContext(Dispatchers.Main) {
                        callback.onFinalizeFailure(result.code, result.message)
                    }
                }
            }
        }
    }
    
    /**
     * Clear session data (call on logout).
     * Also clears any active session token.
     */
    fun clearSessionData() {
        sessionData = null
        SessionTokenManager.clearToken()
        if (enableLogging) {
            android.util.Log.d("ApplePaySDK", "Session data and token cleared")
        }
    }
    
    // Internal methods
    
    internal fun isLoggingEnabled(): Boolean = enableLogging
    
    internal fun getSessionData(): SessionData? = sessionData
    
    /**
     * Get the current session token from /api/init.
     * This is used by Payment SDK for validate/finalize endpoints.
     * Returns null if no token exists.
     */
    fun getSessionToken(): String? {
        return SessionTokenManager.getToken()
    }
    
    private fun ensureInitialized() {
        require(isInitialized) {
            "ApplePaySDK not initialized. Call ApplePaySDK.initialize() first."
        }
    }
    
    private fun ensureSessionData() {
        require(sessionData != null) {
            "Session data not set. Call ApplePaySDK.setSessionData() first."
        }
    }
}
