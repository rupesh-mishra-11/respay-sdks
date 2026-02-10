package com.amenitypay.sdk.models

import com.google.gson.annotations.SerializedName

/**
 * Represents a saved customer payment account (Credit/Debit Card)
 */
data class CustomerPaymentAccount(
    @SerializedName("customer_payment_account_id")
    val customerPaymentAccountId: Long,
    
    @SerializedName("payment_type_id")
    val paymentTypeId: Long,
    
    @SerializedName("payment_type_name")
    val paymentTypeName: String?,
    
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
) {
    val maskedNumber: String
        get() = "•••• •••• •••• $lastFour"
    
    val expiryDate: String
        get() = String.format("%02d/%02d", expiryMonth, expiryYear % 100)
    
    val isExpired: Boolean
        get() {
            val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
            val currentMonth = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1
            return expiryYear < currentYear || (expiryYear == currentYear && expiryMonth < currentMonth)
        }
}

/**
 * Supported card types
 */
enum class CardType(val displayName: String) {
    @SerializedName("visa")
    VISA("Visa"),
    
    @SerializedName("mastercard")
    MASTERCARD("Mastercard"),
    
    @SerializedName("amex")
    AMEX("American Express"),
    
    @SerializedName("discover")
    DISCOVER("Discover"),
    
    @SerializedName("unknown")
    UNKNOWN("Card")
}

/**
 * Response wrapper for customer payment accounts list
 */
data class CustomerPaymentAccountsResponse(
    @SerializedName("payment_accounts")
    val paymentAccounts: List<CustomerPaymentAccount>,
    @SerializedName("total_count")
    val totalCount: Int
)

/**
 * Request model for adding a new payment account
 */
data class AddPaymentAccountRequest(
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
