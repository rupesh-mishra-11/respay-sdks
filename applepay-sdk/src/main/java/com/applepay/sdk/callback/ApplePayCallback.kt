package com.applepay.sdk.callback

/**
 * Callback interface for Apple Pay initialization flow.
 * 
 * Note: Session tokens are managed internally by the SDK.
 * Host app never receives or manages tokens.
 */
interface ApplePayCallback {
    /**
     * Called when Apple Pay initialization succeeds.
     * 
     * The session token is stored internally by the SDK.
     * Use validateApplePay() and finalizeApplePay() to continue the payment flow.
     */
    fun onInitSuccess()
    
    /**
     * Called when initialization fails.
     * 
     * @param errorCode Error code for programmatic handling
     * @param message User-friendly error message
     */
    fun onFailure(errorCode: String, message: String)
    
    /**
     * Called when user cancels the Apple Pay flow.
     */
    fun onCancelled()
}

/**
 * Callback interface for Apple Pay validation.
 */
interface ApplePayValidateCallback {
    /**
     * Called when validation succeeds.
     * 
     * @param transactionId Transaction ID from validation response
     */
    fun onValidateSuccess(transactionId: String)
    
    /**
     * Called when validation fails.
     * 
     * @param errorCode Error code for programmatic handling
     * @param message User-friendly error message
     */
    fun onValidateFailure(errorCode: String, message: String)
}

/**
 * Callback interface for Apple Pay finalization.
 */
interface ApplePayFinalizeCallback {
    /**
     * Called when finalization succeeds.
     * 
     * @param transactionId Transaction ID
     * @param confirmationNumber Confirmation number
     * @param amount Payment amount
     * @param currency Payment currency
     */
    fun onFinalizeSuccess(
        transactionId: String,
        confirmationNumber: String?,
        amount: Double,
        currency: String
    )
    
    /**
     * Called when finalization fails.
     * 
     * @param errorCode Error code for programmatic handling
     * @param message User-friendly error message
     */
    fun onFinalizeFailure(errorCode: String, message: String)
}
