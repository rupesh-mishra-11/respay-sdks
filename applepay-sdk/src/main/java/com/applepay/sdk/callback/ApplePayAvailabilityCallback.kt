package com.applepay.sdk.callback

import com.applepay.sdk.models.ApplePayWalletInfo

/**
 * Callback interface for Apple Pay availability check.
 */
interface ApplePayAvailabilityCallback {
    /**
     * Called when Apple Pay is available and enabled.
     * 
     * @param walletInfo Wallet information including fees, supported networks, etc.
     */
    fun onAvailable(walletInfo: ApplePayWalletInfo)
    
    /**
     * Called when Apple Pay is not available (business rule, not an error).
     * 
     * @param message User-friendly message explaining why Apple Pay is unavailable.
     *                This is the error.message from the API response, NOT debug_message.
     */
    fun onUnavailable(message: String)
    
    /**
     * Called when a network or system error occurs.
     * 
     * @param errorCode Error code for programmatic handling
     * @param message User-friendly error message
     */
    fun onError(errorCode: String, message: String)
}
