package com.amenitypay.sdk.models

import com.google.gson.annotations.SerializedName
import java.io.Serializable

/**
 * Authentication credentials passed from host app
 * Contains client, property, customer, and lease information
 */
data class AuthCredentials(
    @SerializedName("client_id")
    val clientId: Long,
    
    @SerializedName("property_id")
    val propertyId: Long,
    
    @SerializedName("customer_id")
    val customerId: Long,
    
    @SerializedName("lease_id")
    val leaseId: Long
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}

/**
 * Response model for authentication/session
 */
data class AuthResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("session_token")
    val sessionToken: String?,
    @SerializedName("expires_in")
    val expiresIn: Long?,
    @SerializedName("message")
    val message: String?
)

/**
 * User/Customer profile information
 */
data class CustomerProfile(
    @SerializedName("customer_id")
    val customerId: Long,
    @SerializedName("client_id")
    val clientId: Long,
    @SerializedName("property_id")
    val propertyId: Long,
    @SerializedName("lease_id")
    val leaseId: Long,
    @SerializedName("first_name")
    val firstName: String,
    @SerializedName("last_name")
    val lastName: String,
    @SerializedName("email")
    val email: String?,
    @SerializedName("phone")
    val phone: String?
) {
    val fullName: String
        get() = "$firstName $lastName"
}
