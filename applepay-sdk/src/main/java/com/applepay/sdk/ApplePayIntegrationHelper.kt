package com.applepay.sdk

import android.app.Activity
import android.view.View
import com.applepay.sdk.callback.ApplePayAvailabilityCallback
import com.applepay.sdk.callback.ApplePayCallback
import com.applepay.sdk.models.ApplePayWalletInfo

/**
 * Helper class for integrating Apple Pay into host app UI.
 * 
 * This provides convenience methods for common integration patterns.
 * 
 * Note: The radio button UI component is the responsibility of the host app.
 * The SDK only handles the Apple Pay flow once the button is clicked.
 */
object ApplePayIntegrationHelper {
    
    /**
     * Check Apple Pay availability and show/hide the view accordingly.
     * 
     * This is a convenience method that:
     * 1. Calls checkAvailability()
     * 2. Shows the view if available
     * 3. Hides the view if unavailable
     * 
     * @param amount Payment amount
     * @param view The view (e.g., RadioButton) to show/hide based on availability
     * @param callback Optional callback for additional handling
     * @param showOnErrorForTesting If true, shows the button even on network errors (for development/testing only)
     */
    fun checkAndShowApplePayOption(
        amount: Double,
        view: View,
        callback: ApplePayAvailabilityCallback? = null,
        showOnErrorForTesting: Boolean = false
    ) {
        // Hide by default - callbacks from checkAvailability are already on main thread
        view.visibility = View.GONE
        
        ApplePaySDK.checkAvailability(amount, object : ApplePayAvailabilityCallback {
            override fun onAvailable(walletInfo: ApplePayWalletInfo) {
                // Callbacks are already on main thread via withContext(Dispatchers.Main)
                view.visibility = View.VISIBLE
                android.util.Log.d("ApplePayHelper", "Setting view visibility to VISIBLE")
                callback?.onAvailable(walletInfo)
            }
            
            override fun onUnavailable(message: String) {
                view.visibility = View.GONE
                android.util.Log.d("ApplePayHelper", "Setting view visibility to GONE - unavailable: $message")
                callback?.onUnavailable(message)
            }
            
            override fun onError(errorCode: String, message: String) {
                if (showOnErrorForTesting) {
                    // For development/testing: show button even on network errors
                    view.visibility = View.VISIBLE
                    android.util.Log.w("ApplePayHelper", "⚠️ TESTING MODE: Showing button despite error: $errorCode - $message")
                } else {
                    view.visibility = View.GONE
                    android.util.Log.e("ApplePayHelper", "Setting view visibility to GONE - error: $errorCode - $message")
                }
                callback?.onError(errorCode, message)
            }
        })
    }
    
    /**
     * Setup Apple Pay view click handler.
     * 
     * This connects the view click to the Apple Pay flow:
     * 1. When clicked, directly starts Apple Pay initialization (calls /api/init)
     * 2. Availability is checked as part of the initialization process
     * 3. If unavailable or error, shows appropriate message via callback
     * 
     * Note: This method calls /api/init only once when the button is clicked.
     * For checking availability to show/hide the button, use checkAndShowApplePayOption() separately.
     * 
     * @param activity Calling activity
     * @param amount Payment amount
     * @param view The view (e.g., RadioButton) that triggers Apple Pay
     * @param callback Callback for Apple Pay initialization result
     * @param availabilityCallback Optional callback for availability check result (called on failure)
     * @param onLoadingStart Optional callback called when API call starts (for showing loader)
     */
    fun setupApplePayClickHandler(
        activity: Activity,
        amount: Double,
        view: View,
        callback: ApplePayCallback,
        availabilityCallback: ApplePayAvailabilityCallback? = null,
        onLoadingStart: (() -> Unit)? = null
    ) {
        view.setOnClickListener {
            // Show loader if callback provided
            onLoadingStart?.invoke()
            
            // Directly start Apple Pay flow - startApplePay() already calls /api/init
            // No need to call checkAvailability() first as it causes duplicate API calls
            ApplePaySDK.startApplePay(activity, amount, object : ApplePayCallback {
                override fun onInitSuccess() {
                    // Extract wallet info from the response if needed for availability callback
                    // Note: startApplePay() already validated availability via /api/init
                    callback.onInitSuccess()
                }
                
                override fun onFailure(errorCode: String, message: String) {
                    // Map failure to availability callback if needed
                    when (errorCode) {
                        "APPLE_PAY_DISABLED" -> {
                            availabilityCallback?.onUnavailable(message)
                        }
                        else -> {
                            availabilityCallback?.onError(errorCode, message)
                        }
                    }
                    callback.onFailure(errorCode, message)
                }
                
                override fun onCancelled() {
                    callback.onCancelled()
                }
            })
        }
    }
}
