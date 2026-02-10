package com.applepay.sdk.models

import com.google.gson.annotations.SerializedName
import java.io.Serializable

/**
 * Request model for /api/validate endpoint.
 */
data class ApplePayValidateRequest(
    @SerializedName("mfe")
    val mfe: String = "applepay",
    
    @SerializedName("data")
    val data: ValidateRequestData
) {
    data class ValidateRequestData(
        @SerializedName("payment_data")
        val paymentData: String
    )
}

/**
 * Response model for /api/validate endpoint.
 */
data class ApplePayValidateResponse(
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
data class ApplePayFinalizeRequest(
    @SerializedName("mfe")
    val mfe: String = "applepay",
    
    @SerializedName("data")
    val data: FinalizeRequestData
) {
    data class FinalizeRequestData(
        @SerializedName("transaction_id")
        val transactionId: String
    )
}

/**
 * Response model for /api/finalize endpoint.
 */
data class ApplePayFinalizeResponse(
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
