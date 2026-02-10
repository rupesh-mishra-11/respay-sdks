package com.payment.sdk.models

import com.google.gson.annotations.SerializedName
import java.io.Serializable

// Re-export PaymentMethod and CardType from shared SDK for convenience
typealias PaymentMethod = com.paymentmethod.sdk.models.PaymentMethod
typealias CardType = com.paymentmethod.sdk.models.CardType

/**
 * Payment request passed from host app to SDK.
 */
data class PaymentRequest(
    val amount: Double,
    val currency: String = "USD",
    val description: String? = null
) : Serializable

/**
 * Request to add a new payment method.
 */
data class AddCardRequest(
    val cardNumber: String,
    val expiryMonth: Int,
    val expiryYear: Int,
    val cvv: String,
    val cardHolderName: String
)

/**
 * Request to process a payment.
 */
data class ProcessPaymentRequest(
    @SerializedName("customer_id")
    val customerId: Long,
    
    @SerializedName("payment_method_id")
    val paymentMethodId: Long,
    
    @SerializedName("amount")
    val amount: Double,
    
    @SerializedName("currency")
    val currency: String,
    
    @SerializedName("description")
    val description: String?
)

/**
 * Payment transaction response.
 */
data class PaymentTransaction(
    @SerializedName("transaction_id")
    val transactionId: String,
    
    @SerializedName("status")
    val status: TransactionStatus,
    
    @SerializedName("amount")
    val amount: Double,
    
    @SerializedName("currency")
    val currency: String,
    
    @SerializedName("confirmation_number")
    val confirmationNumber: String?,
    
    @SerializedName("message")
    val message: String?
)

enum class TransactionStatus {
    @SerializedName("success") SUCCESS,
    @SerializedName("failed") FAILED,
    @SerializedName("pending") PENDING
}

/**
 * Result returned to host app after payment flow.
 */
sealed class PaymentResult : Serializable {
    
    data class Success(
        val transactionId: String,
        val confirmationNumber: String?,
        val amount: Double,
        val currency: String
    ) : PaymentResult()
    
    data class Failure(
        val errorCode: String,
        val errorMessage: String
    ) : PaymentResult()
    
    object Cancelled : PaymentResult()
}

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

data class ApiError(
    @SerializedName("code")
    val code: String,
    
    @SerializedName("message")
    val message: String
)

/**
 * Request model for /api/validate endpoint.
 */
data class ValidateRequest(
    @SerializedName("mfe")
    val mfe: String = "applepay",
    
    @SerializedName("data")
    val data: ValidateRequestData
) {
    data class ValidateRequestData(
        @SerializedName("validation_url")
        val validationUrl: String,
        
        @SerializedName("cid")
        val cid: Long,
        
        @SerializedName("property_id")
        val propertyId: Long,
        
        @SerializedName("customer_id")
        val customerId: Long,
        
        @SerializedName("lease_id")
        val leaseId: Long,
        
        @SerializedName("amount")
        val amount: Double
    )
}

/**
 * Response model for /api/validate endpoint.
 */
data class ValidateResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("statusCode")
    val statusCode: Int,
    
    @SerializedName("data")
    val data: ValidateResponseData?,
    
    @SerializedName("error")
    val error: ApiError?
) {
    data class ValidateResponseData(
        @SerializedName("validated")
        val validated: Boolean?,
        
        @SerializedName("transaction_id")
        val transactionId: String?
    )
}

/**
 * Request model for /api/finalize endpoint.
 */
data class FinalizeRequest(
    @SerializedName("mfe")
    val mfe: String = "applepay",
    
    @SerializedName("data")
    val data: FinalizeRequestData
) {
    data class FinalizeRequestData(
        @SerializedName("cid")
        val cid: Long,
        
        @SerializedName("property_id")
        val propertyId: Long,
        
        @SerializedName("customer_id")
        val customerId: Long,
        
        @SerializedName("lease_id")
        val leaseId: Long,
        
        @SerializedName("amount")
        val amount: Double,
        
        @SerializedName("payment_token")
        val paymentToken: PaymentToken,
        
        @SerializedName("bill_to_customer")
        val billToCustomer: BillToCustomer,
        
        @SerializedName("total_amount")
        val totalAmount: String,
        
        @SerializedName("payment_amounts")
        val paymentAmounts: PaymentAmounts,
        
        @SerializedName("agrees_to_fees")
        val agreesToFees: Boolean,
        
        @SerializedName("agrees_to_terms")
        val agreesToTerms: Boolean,
        
        @SerializedName("terms_accepted_on")
        val termsAcceptedOn: String,
        
        @SerializedName("provider")
        val provider: String
    )
    
    data class PaymentToken(
        @SerializedName("paymentData")
        val paymentData: PaymentData,
        
        @SerializedName("paymentMethod")
        val paymentMethod: PaymentMethodTokenInfo,
        
        @SerializedName("transactionIdentifier")
        val transactionIdentifier: String
    ) {
        data class PaymentData(
            @SerializedName("data")
            val data: String,
            
            @SerializedName("signature")
            val signature: String,
            
            @SerializedName("header")
            val header: PaymentHeader,
            
            @SerializedName("version")
            val version: String
        ) {
            data class PaymentHeader(
                @SerializedName("publicKeyHash")
                val publicKeyHash: String,
                
                @SerializedName("ephemeralPublicKey")
                val ephemeralPublicKey: String,
                
                @SerializedName("transactionId")
                val transactionId: String
            )
        }
        
        data class PaymentMethodTokenInfo(
            @SerializedName("displayName")
            val displayName: String,
            
            @SerializedName("network")
            val network: String,
            
            @SerializedName("type")
            val type: String
        )
    }
    
    data class BillToCustomer(
        @SerializedName("first_name")
        val firstName: String,
        
        @SerializedName("last_name")
        val lastName: String,
        
        @SerializedName("postal_code")
        val postalCode: String,
        
        @SerializedName("country_code")
        val countryCode: String,
        
        @SerializedName("email")
        val email: String,
        
        @SerializedName("city")
        val city: String,
        
        @SerializedName("state")
        val state: String,
        
        @SerializedName("address_line_1")
        val addressLine1: String
    )
    
    data class PaymentAmounts(
        @SerializedName("amount")
        val amount: String,
        
        @SerializedName("convenience_fee")
        val convenienceFee: String
    )
}

/**
 * Response model for /api/finalize endpoint.
 */
data class FinalizeResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("statusCode")
    val statusCode: Int,
    
    @SerializedName("data")
    val data: FinalizeResponseData?,
    
    @SerializedName("error")
    val error: ApiError?
) {
    data class FinalizeResponseData(
        @SerializedName("transaction_id")
        val transactionId: String?,
        
        @SerializedName("confirmation_number")
        val confirmationNumber: String?,
        
        @SerializedName("amount")
        val amount: Double?,
        
        @SerializedName("currency")
        val currency: String?
    )
}

/**
 * Internal result wrapper.
 */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val code: String, val message: String) : Result<Nothing>()
    object Loading : Result<Nothing>()
    
    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
}
