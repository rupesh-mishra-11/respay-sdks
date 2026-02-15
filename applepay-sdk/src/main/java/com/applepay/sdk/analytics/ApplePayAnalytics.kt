package com.applepay.sdk.analytics

import android.content.Context
import com.applepay.sdk.ApplePaySDK
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase

/**
 * Firebase Analytics helper for Apple Pay SDK.
 * 
 * Tracks key events throughout the Apple Pay payment flow:
 * - SDK initialization
 * - Availability checks
 * - Payment initialization
 * - Payment validation
 * - Payment finalization
 * - Errors
 * 
 * All analytics tracking is non-blocking and safe to call from any thread.
 */
internal object ApplePayAnalytics {
    
    private var firebaseAnalytics: FirebaseAnalytics? = null
    private var isEnabled: Boolean = true
    
    /**
     * Initialize Firebase Analytics.
     * Should be called during SDK initialization.
     * 
     * @param context Application context
     */
    fun initialize(context: Context) {
        try {
            firebaseAnalytics = Firebase.analytics
            logEvent("applepay_sdk_initialized", mapOf(
                "sdk_version" to ApplePaySDK.VERSION
            ))
        } catch (e: Exception) {
            // Firebase Analytics might not be initialized in the host app
            // Silently fail to avoid breaking SDK functionality
            isEnabled = false
            if (ApplePaySDK.isLoggingEnabled()) {
                android.util.Log.w("ApplePayAnalytics", "Firebase Analytics not available", e)
            }
        }
    }
    
    /**
     * Track SDK initialization event.
     */
    fun trackSDKInitialized(environment: String) {
        logEvent("applepay_sdk_initialized", mapOf(
            "environment" to environment,
            "sdk_version" to ApplePaySDK.VERSION
        ))
    }
    
    /**
     * Track session data set event.
     */
    fun trackSessionDataSet(cid: Int, propertyId: Int, customerId: Long, leaseId: Long) {
        logEvent("applepay_session_data_set", mapOf(
            "cid" to cid.toString(),
            "property_id" to propertyId.toString(),
            "customer_id" to customerId.toString(),
            "lease_id" to leaseId.toString()
        ))
    }
    
    /**
     * Track availability check event.
     */
    fun trackAvailabilityCheck(amount: Double, isAvailable: Boolean, errorCode: String? = null) {
        val params = mutableMapOf<String, Any>(
            "amount" to amount,
            "available" to (if (isAvailable) "true" else "false")
        )
        errorCode?.let { params["error_code"] = it }
        
        logEvent("applepay_availability_check", params)
    }
    
    /**
     * Track payment initialization start event.
     */
    fun trackPaymentInitStart(amount: Double) {
        logEvent("applepay_payment_init_start", mapOf(
            "amount" to amount
        ))
    }
    
    /**
     * Track payment initialization success event.
     */
    fun trackPaymentInitSuccess(amount: Double) {
        logEvent("applepay_payment_init_success", mapOf(
            "amount" to amount
        ))
    }
    
    /**
     * Track payment initialization failure event.
     */
    fun trackPaymentInitFailure(amount: Double, errorCode: String, errorMessage: String) {
        logEvent("applepay_payment_init_failure", mapOf(
            "amount" to amount,
            "error_code" to errorCode,
            "error_message" to errorMessage
        ))
    }
    
    /**
     * Track payment initialization cancellation event.
     */
    fun trackPaymentInitCancelled(amount: Double) {
        logEvent("applepay_payment_init_cancelled", mapOf(
            "amount" to amount
        ))
    }
    
    /**
     * Track payment validation start event.
     */
    fun trackPaymentValidateStart() {
        logEvent("applepay_payment_validate_start", emptyMap())
    }
    
    /**
     * Track payment validation success event.
     */
    fun trackPaymentValidateSuccess(transactionId: String) {
        logEvent("applepay_payment_validate_success", mapOf(
            "transaction_id" to transactionId
        ))
    }
    
    /**
     * Track payment validation failure event.
     */
    fun trackPaymentValidateFailure(errorCode: String, errorMessage: String) {
        logEvent("applepay_payment_validate_failure", mapOf(
            "error_code" to errorCode,
            "error_message" to errorMessage
        ))
    }
    
    /**
     * Track payment finalization start event.
     */
    fun trackPaymentFinalizeStart(transactionId: String) {
        logEvent("applepay_payment_finalize_start", mapOf(
            "transaction_id" to transactionId
        ))
    }
    
    /**
     * Track payment finalization success event.
     */
    fun trackPaymentFinalizeSuccess(
        transactionId: String,
        confirmationNumber: String?,
        amount: Double,
        currency: String
    ) {
        val params = mutableMapOf<String, Any>(
            "transaction_id" to transactionId,
            "amount" to amount,
            "currency" to currency
        )
        confirmationNumber?.let { params["confirmation_number"] = it }
        
        logEvent("applepay_payment_finalize_success", params)
    }
    
    /**
     * Track payment finalization failure event.
     */
    fun trackPaymentFinalizeFailure(transactionId: String, errorCode: String, errorMessage: String) {
        logEvent("applepay_payment_finalize_failure", mapOf(
            "transaction_id" to transactionId,
            "error_code" to errorCode,
            "error_message" to errorMessage
        ))
    }
    
    /**
     * Track session data cleared event.
     */
    fun trackSessionDataCleared() {
        logEvent("applepay_session_data_cleared", emptyMap())
    }
    
    /**
     * Track generic error event.
     */
    fun trackError(errorCode: String, errorMessage: String, context: String) {
        logEvent("applepay_error", mapOf(
            "error_code" to errorCode,
            "error_message" to errorMessage,
            "context" to context
        ))
    }
    
    /**
     * Internal method to log events safely.
     * Handles all exceptions to prevent breaking SDK functionality.
     */
    private fun logEvent(eventName: String, parameters: Map<String, Any>) {
        if (!isEnabled || firebaseAnalytics == null) {
            return
        }
        
        try {
            val bundle = android.os.Bundle().apply {
                parameters.forEach { (key, value) ->
                    when (value) {
                        is String -> putString(key, value)
                        is Number -> putDouble(key, value.toDouble())
                        is Boolean -> putString(key, value.toString())
                        else -> putString(key, value.toString())
                    }
                }
            }
            
            firebaseAnalytics?.logEvent(eventName, bundle)
        } catch (e: Exception) {
            // Silently fail to avoid breaking SDK functionality
            if (ApplePaySDK.isLoggingEnabled()) {
                android.util.Log.w("ApplePayAnalytics", "Failed to log event: $eventName", e)
            }
        }
    }
    
    /**
     * Enable or disable analytics tracking.
     * Useful for testing or privacy compliance.
     */
    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
    }
    
    /**
     * Check if analytics is enabled.
     */
    fun isEnabled(): Boolean = isEnabled && firebaseAnalytics != null
    
    /**
     * Public method to track payment cancellation.
     * Can be called from host app's ApplePayCallback.onCancelled() implementation.
     * 
     * @param amount Payment amount that was cancelled
     */
    fun trackPaymentCancellation(amount: Double) {
        trackPaymentInitCancelled(amount)
    }
}
