package com.applepay.sdk.models

import com.google.gson.annotations.SerializedName
import java.io.Serializable

/**
 * Request model for /api/init endpoint.
 */
data class ApplePayInitRequest(
    @SerializedName("mfe")
    val mfe: String = "applepay",
    
    @SerializedName("data")
    val data: InitRequestData
) {
    data class InitRequestData(
        @SerializedName("cid")
        val cid: Int,
        
        @SerializedName("property_id")
        val propertyId: Int,
        
        @SerializedName("customer_id")
        val customerId: Long,
        
        @SerializedName("lease_id")
        val leaseId: Long,
        
        @SerializedName("amount")
        val amount: Double
    )
}

/**
 * Response model for /api/init endpoint.
 */
data class ApplePayInitResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("statusCode")
    val statusCode: Int,
    
    @SerializedName("data")
    val data: InitResponseData?,
    
    @SerializedName("error")
    val error: ApiError?,
    
    @SerializedName("token")
    val token: String?
) {
    data class InitResponseData(
        @SerializedName("status")
        val status: String?,
        
        @SerializedName("is_applepay_enabled")
        val isApplePayEnabled: Boolean?,
        
        @SerializedName("wallet_settings")
        val walletSettings: List<WalletSetting>?
    )
    
    /**
     * Check if Apple Pay is available based on response.
     */
    fun isApplePayAvailable(): Boolean {
        if (!success || statusCode != 200) return false
        if (data?.isApplePayEnabled != true) return false
        
        val applePaySetting = data.walletSettings?.find { it.name == "apple_pay" }
        return applePaySetting?.isEnabled == true
    }
}

/**
 * Wallet setting from BFF response.
 */
data class WalletSetting(
    @SerializedName("name")
    val name: String,
    
    @SerializedName("is_enabled")
    val isEnabled: Boolean,
    
    @SerializedName("fees")
    val fees: WalletFees?,
    
    @SerializedName("supported_networks")
    val supportedNetworks: List<String>?,
    
    @SerializedName("merchant_capabilities")
    val merchantCapabilities: List<String>?,
    
    @SerializedName("supported_card_type")
    val supportedCardType: List<String>?,
    
    @SerializedName("billing_info")
    val billingInfo: Boolean?
) : Serializable

/**
 * Wallet fees structure.
 */
data class WalletFees(
    @SerializedName("debit")
    val debit: Double?,
    
    @SerializedName("credit")
    val credit: Double?
) : Serializable

/**
 * API error response.
 */
data class ApiError(
    @SerializedName("message")
    val message: String,
    
    @SerializedName("debug_message")
    val debugMessage: String?,
    
    @SerializedName("code")
    val code: String?
) : Serializable

/**
 * Wallet information exposed to host app (read-only).
 * 
 * Note: This is used for availability checks only.
 * Session tokens are managed internally by the SDK and never exposed to the host app.
 */
data class ApplePayWalletInfo(
    val isEnabled: Boolean,
    val walletSetting: WalletSetting?
) : Serializable {
    companion object {
        fun fromResponse(response: ApplePayInitResponse): ApplePayWalletInfo? {
            if (!response.isApplePayAvailable()) return null
            
            val applePaySetting = response.data?.walletSettings?.find { it.name == "apple_pay" }
            return ApplePayWalletInfo(
                isEnabled = true,
                walletSetting = applePaySetting
            )
        }
    }
}
