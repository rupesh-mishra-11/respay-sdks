package com.amenitypay.sdk.callback

import com.amenitypay.sdk.models.AmenityPayResult

/**
 * Callback interface for AmenityPay SDK results
 */
interface AmenityPayCallback {
    
    /**
     * Called when payment is successful
     *
     * @param result Contains transaction details including transactionId, confirmationNumber, amount, etc.
     */
    fun onPaymentSuccess(result: AmenityPayResult.Success)
    
    /**
     * Called when payment fails
     *
     * @param result Contains error code and message
     */
    fun onPaymentFailure(result: AmenityPayResult.Failure)
    
    /**
     * Called when user cancels the payment flow
     */
    fun onPaymentCancelled()
}

/**
 * Extension function to handle AmenityPayResult with the callback
 */
fun AmenityPayCallback.handleResult(result: AmenityPayResult) {
    when (result) {
        is AmenityPayResult.Success -> onPaymentSuccess(result)
        is AmenityPayResult.Failure -> onPaymentFailure(result)
        is AmenityPayResult.Cancelled -> onPaymentCancelled()
    }
}
