package com.paymentmethod.sdk.models

import com.google.gson.annotations.SerializedName
import java.io.Serializable
import java.util.Calendar

/**
 * Represents a saved payment method (Credit/Debit Card).
 * This is the unified model used across all SDKs.
 */
data class PaymentMethod(
    @SerializedName("id")
    val id: Long,
    
    @SerializedName("payment_type_id")
    val paymentTypeId: Long = 0,
    
    @SerializedName("card_type")
    val cardType: CardType,
    
    @SerializedName("last_four")
    val lastFour: String,
    
    @SerializedName("expiry_month")
    val expiryMonth: Int,
    
    @SerializedName("expiry_year")
    val expiryYear: Int,
    
    @SerializedName("card_holder_name")
    val cardHolderName: String,
    
    @SerializedName("is_default")
    val isDefault: Boolean = false,
    
    @SerializedName("is_active")
    val isActive: Boolean = true
) : Serializable {
    
    val maskedNumber: String
        get() = "•••• •••• •••• $lastFour"
    
    val expiryDate: String
        get() = String.format("%02d/%02d", expiryMonth, expiryYear % 100)
    
    val isExpired: Boolean
        get() {
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
            return expiryYear < currentYear || (expiryYear == currentYear && expiryMonth < currentMonth)
        }
}

/**
 * Supported card types.
 */
enum class CardType(val displayName: String) {
    @SerializedName("visa") VISA("Visa"),
    @SerializedName("mastercard") MASTERCARD("Mastercard"),
    @SerializedName("amex") AMEX("American Express"),
    @SerializedName("discover") DISCOVER("Discover"),
    @SerializedName("unknown") UNKNOWN("Card")
}

/**
 * Request to add a new payment method.
 */
data class AddPaymentMethodRequest(
    @SerializedName("customer_id")
    val customerId: Long,
    
    @SerializedName("card_number")
    val cardNumber: String,
    
    @SerializedName("expiry_month")
    val expiryMonth: Int,
    
    @SerializedName("expiry_year")
    val expiryYear: Int,
    
    @SerializedName("cvv")
    val cvv: String,
    
    @SerializedName("card_holder_name")
    val cardHolderName: String,
    
    @SerializedName("set_as_default")
    val setAsDefault: Boolean = false
)

/**
 * Response wrapper for payment methods list.
 */
data class PaymentMethodsResponse(
    @SerializedName("payment_methods")
    val paymentMethods: List<PaymentMethod>,
    
    @SerializedName("total_count")
    val totalCount: Int
)

/**
 * Generic API response wrapper.
 */
data class ApiResponse<T>(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("data")
    val data: T?,
    
    @SerializedName("error")
    val error: ApiError?
)

/**
 * API error details.
 */
data class ApiError(
    @SerializedName("code")
    val code: String,
    
    @SerializedName("message")
    val message: String
)

/**
 * Internal result wrapper for operations.
 */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val code: String, val message: String) : Result<Nothing>()
    object Loading : Result<Nothing>()
    
    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
}
