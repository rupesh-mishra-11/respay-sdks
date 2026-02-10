package com.paymentmethod.sdk

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.annotation.StyleRes
import com.paymentmethod.sdk.callback.PaymentMethodCallback
import com.paymentmethod.sdk.config.CustomerCredentials
import com.paymentmethod.sdk.config.PaymentMethodConfig
import com.paymentmethod.sdk.config.StyleConfig
import com.paymentmethod.sdk.models.PaymentMethod
import com.paymentmethod.sdk.network.NetworkClient
import com.paymentmethod.sdk.repository.PaymentMethodRepository
import com.paymentmethod.sdk.ui.PaymentMethodSelectorActivity

/**
 * Main entry point for the PaymentMethod SDK.
 * 
 * Usage:
 * 1. Initialize in Application class: PaymentMethodSDK.initialize(config)
 * 2. Set customer when logged in: PaymentMethodSDK.setCustomer(customerId, authToken)
 * 3. Launch selector: PaymentMethodSDK.selectPaymentMethod(activity, callback)
 */
object PaymentMethodSDK {
    
    private var config: PaymentMethodConfig? = null
    private var credentials: CustomerCredentials? = null
    private var callback: PaymentMethodCallback? = null
    private var repository: PaymentMethodRepository? = null
    private var styleConfig: StyleConfig = StyleConfig.DEFAULT
    private var customThemeRes: Int? = null
    
    /**
     * Initialize the SDK with configuration.
     * Call this in your Application.onCreate().
     */
    fun initialize(config: PaymentMethodConfig) {
        this.config = config
        
        NetworkClient.initialize(
            config = config,
            authTokenProvider = { credentials?.authToken }
        )
        
        repository = PaymentMethodRepository()
    }
    
    /**
     * Initialize with builder pattern for convenience.
     */
    fun initialize(builder: PaymentMethodConfig.Builder.() -> Unit) {
        val configBuilder = PaymentMethodConfig.Builder()
        configBuilder.builder()
        initialize(configBuilder.build())
    }
    
    /**
     * Set custom style configuration for the SDK.
     * Call this after initialize() to customize the SDK appearance.
     * 
     * @param styleConfig The style configuration
     */
    fun setStyle(styleConfig: StyleConfig) {
        this.styleConfig = styleConfig
        this.customThemeRes = styleConfig.customTheme
    }
    
    /**
     * Set custom style using builder pattern.
     */
    fun setStyle(builder: StyleConfig.Builder.() -> Unit) {
        val styleBuilder = StyleConfig.Builder()
        styleBuilder.builder()
        setStyle(styleBuilder.build())
    }
    
    /**
     * Set a custom theme resource from the host app.
     * The theme should extend PaymentMethodSDKTheme.
     * 
     * @param themeRes Theme resource ID (e.g., R.style.MyCustomPaymentTheme)
     */
    fun setCustomTheme(@StyleRes themeRes: Int) {
        this.customThemeRes = themeRes
    }
    
    /**
     * Set the customer credentials after login.
     */
    fun setCustomer(customerId: Long, authToken: String) {
        credentials = CustomerCredentials(customerId, authToken)
    }
    
    /**
     * Clear the customer credentials on logout.
     */
    fun clearCustomer() {
        credentials = null
    }
    
    /**
     * Launch the payment method selector activity.
     * @param activity The activity to launch from
     * @param callback Callback for receiving the result
     * @param requestCode Request code for startActivityForResult
     */
    fun selectPaymentMethod(
        activity: Activity,
        callback: PaymentMethodCallback,
        requestCode: Int = REQUEST_SELECT_PAYMENT_METHOD
    ) {
        validateInitialized()
        validateCustomer()
        
        this.callback = callback
        
        val intent = Intent(activity, PaymentMethodSelectorActivity::class.java).apply {
            putExtra(
                PaymentMethodSelectorActivity.EXTRA_SELECTION_MODE,
                PaymentMethodSelectorActivity.SelectionMode.SELECT.name
            )
        }
        activity.startActivityForResult(intent, requestCode)
    }
    
    /**
     * Launch the add payment method dialog directly.
     * @param activity The activity to launch from
     * @param callback Callback for receiving the result
     * @param requestCode Request code for startActivityForResult
     */
    fun addPaymentMethod(
        activity: Activity,
        callback: PaymentMethodCallback,
        requestCode: Int = REQUEST_ADD_PAYMENT_METHOD
    ) {
        validateInitialized()
        validateCustomer()
        
        this.callback = callback
        
        val intent = Intent(activity, PaymentMethodSelectorActivity::class.java).apply {
            putExtra(
                PaymentMethodSelectorActivity.EXTRA_SELECTION_MODE,
                PaymentMethodSelectorActivity.SelectionMode.ADD_ONLY.name
            )
        }
        activity.startActivityForResult(intent, requestCode)
    }
    
    /**
     * Launch the payment method manager (view and manage saved methods).
     * @param activity The activity to launch from
     * @param callback Callback for receiving updates
     * @param requestCode Request code for startActivityForResult
     */
    fun managePaymentMethods(
        activity: Activity,
        callback: PaymentMethodCallback,
        requestCode: Int = REQUEST_MANAGE_PAYMENT_METHODS
    ) {
        validateInitialized()
        validateCustomer()
        
        this.callback = callback
        
        val intent = Intent(activity, PaymentMethodSelectorActivity::class.java).apply {
            putExtra(
                PaymentMethodSelectorActivity.EXTRA_SELECTION_MODE,
                PaymentMethodSelectorActivity.SelectionMode.MANAGE.name
            )
        }
        activity.startActivityForResult(intent, requestCode)
    }
    
    /**
     * Get an Intent for the payment method selector.
     * Use this if you need more control over launching the activity.
     */
    fun getSelectIntent(context: Context): Intent {
        validateInitialized()
        return Intent(context, PaymentMethodSelectorActivity::class.java).apply {
            putExtra(
                PaymentMethodSelectorActivity.EXTRA_SELECTION_MODE,
                PaymentMethodSelectorActivity.SelectionMode.SELECT.name
            )
        }
    }
    
    // Internal methods
    
    internal fun getRepository(): PaymentMethodRepository {
        return repository ?: throw IllegalStateException(
            "PaymentMethodSDK not initialized. Call PaymentMethodSDK.initialize() first."
        )
    }
    
    internal fun getCustomerId(): Long? = credentials?.customerId
    
    internal fun getAuthToken(): String? = credentials?.authToken
    
    internal fun getStyleConfig(): StyleConfig = styleConfig
    
    internal fun getCustomTheme(): Int? = customThemeRes
    
    internal fun notifyMethodSelected(method: PaymentMethod) {
        callback?.onPaymentMethodSelected(method)
    }
    
    internal fun notifyMethodAdded(method: PaymentMethod) {
        callback?.onPaymentMethodAdded(method)
    }
    
    internal fun notifyCancelled() {
        callback?.onCancelled()
    }
    
    internal fun notifyError(code: String, message: String) {
        callback?.onError(code, message)
    }
    
    private fun validateInitialized() {
        requireNotNull(config) {
            "PaymentMethodSDK not initialized. Call PaymentMethodSDK.initialize() first."
        }
    }
    
    private fun validateCustomer() {
        requireNotNull(credentials) {
            "Customer not set. Call PaymentMethodSDK.setCustomer() first."
        }
    }
    
    // Constants
    
    const val REQUEST_SELECT_PAYMENT_METHOD = 5001
    const val REQUEST_ADD_PAYMENT_METHOD = 5002
    const val REQUEST_MANAGE_PAYMENT_METHODS = 5003
    
    const val EXTRA_SELECTED_PAYMENT_METHOD = PaymentMethodSelectorActivity.EXTRA_SELECTED_PAYMENT_METHOD
}
