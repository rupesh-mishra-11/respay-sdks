package com.paymentmethod.sdk.callback

import com.paymentmethod.sdk.models.PaymentMethod

/**
 * Callback interface for payment method selection results.
 * Implement this to receive results from the PaymentMethodSelector.
 */
interface PaymentMethodCallback {
    
    /**
     * Called when a payment method is selected.
     * @param paymentMethod The selected payment method
     */
    fun onPaymentMethodSelected(paymentMethod: PaymentMethod)
    
    /**
     * Called when a new payment method is added.
     * @param paymentMethod The newly added payment method
     */
    fun onPaymentMethodAdded(paymentMethod: PaymentMethod)
    
    /**
     * Called when the user cancels the selection.
     */
    fun onCancelled()
    
    /**
     * Called when an error occurs.
     * @param code Error code
     * @param message Error message
     */
    fun onError(code: String, message: String)
}

/**
 * Simple adapter for PaymentMethodCallback with default empty implementations.
 * Extend this class to only override the methods you need.
 */
open class SimplePaymentMethodCallback : PaymentMethodCallback {
    override fun onPaymentMethodSelected(paymentMethod: PaymentMethod) {}
    override fun onPaymentMethodAdded(paymentMethod: PaymentMethod) {}
    override fun onCancelled() {}
    override fun onError(code: String, message: String) {}
}
