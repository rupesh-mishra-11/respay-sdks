package com.amenitypay.sdk.models

import com.google.gson.annotations.SerializedName
import java.io.Serializable

/**
 * Amenity booking request passed from host app to SDK
 * Contains all data needed for amenity payment
 */
data class AmenityBookingRequest(
    @SerializedName("amenity_id")
    val amenityId: Long,
    
    @SerializedName("amenity_name")
    val amenityName: String,
    
    @SerializedName("start_datetime")
    val startDatetime: String,  // ISO 8601 format: "2024-01-15T10:00:00Z"
    
    @SerializedName("end_datetime")
    val endDatetime: String,    // ISO 8601 format: "2024-01-15T12:00:00Z"
    
    @SerializedName("amount")
    val amount: Double,
    
    @SerializedName("currency")
    val currency: String = "USD",
    
    @SerializedName("description")
    val description: String? = null
) : Serializable {
    
    companion object {
        private const val serialVersionUID = 1L
    }
}

/**
 * Payment request to be sent to API
 * Combines auth credentials with booking and payment info
 */
data class ProcessPaymentRequest(
    // Auth/Customer Info
    @SerializedName("client_id")
    val clientId: Long,
    
    @SerializedName("property_id")
    val propertyId: Long,
    
    @SerializedName("customer_id")
    val customerId: Long,
    
    @SerializedName("lease_id")
    val leaseId: Long,
    
    // Amenity Info
    @SerializedName("amenity_id")
    val amenityId: Long,
    
    @SerializedName("start_datetime")
    val startDatetime: String,
    
    @SerializedName("end_datetime")
    val endDatetime: String,
    
    // Payment Info
    @SerializedName("customer_payment_account_id")
    val customerPaymentAccountId: Long,
    
    @SerializedName("payment_type_id")
    val paymentTypeId: Long,
    
    @SerializedName("amount")
    val amount: Double,
    
    @SerializedName("currency")
    val currency: String
)

/**
 * Payment transaction response
 */
data class PaymentResponse(
    @SerializedName("transaction_id")
    val transactionId: String,
    
    @SerializedName("status")
    val status: PaymentStatus,
    
    @SerializedName("amount")
    val amount: Double,
    
    @SerializedName("currency")
    val currency: String,
    
    @SerializedName("amenity_id")
    val amenityId: Long,
    
    @SerializedName("customer_payment_account_id")
    val customerPaymentAccountId: Long,
    
    @SerializedName("created_at")
    val createdAt: String,
    
    @SerializedName("confirmation_number")
    val confirmationNumber: String?,
    
    @SerializedName("message")
    val message: String?
)

/**
 * Payment status enum
 */
enum class PaymentStatus(val displayName: String) {
    @SerializedName("success")
    SUCCESS("Successful"),
    
    @SerializedName("pending")
    PENDING("Pending"),
    
    @SerializedName("failed")
    FAILED("Failed"),
    
    @SerializedName("cancelled")
    CANCELLED("Cancelled"),
    
    @SerializedName("refunded")
    REFUNDED("Refunded")
}

/**
 * SDK Result returned to host app
 */
sealed class AmenityPayResult : Serializable {
    
    data class Success(
        val transactionId: String,
        val confirmationNumber: String?,
        val amount: Double,
        val currency: String,
        val amenityId: Long
    ) : AmenityPayResult()
    
    data class Failure(
        val errorCode: String,
        val errorMessage: String
    ) : AmenityPayResult()
    
    data object Cancelled : AmenityPayResult()
    
    companion object {
        private const val serialVersionUID = 1L
    }
}
