package com.payment.sdk.callback

import com.payment.sdk.models.PaymentResult

/**
 * Callback interface for receiving payment results.
 */
interface PaymentCallback {
    
    /**
     * Called when the payment flow completes.
     * 
     * @param result The payment result:
     *   - PaymentResult.Success: Payment completed successfully
     *   - PaymentResult.Failure: Payment failed
     *   - PaymentResult.Cancelled: User cancelled the payment
     */
    fun onPaymentResult(result: PaymentResult)
}
