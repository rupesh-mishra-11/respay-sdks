package com.amenitypay.sdk.repository

import com.amenitypay.sdk.auth.SessionManager
import com.amenitypay.sdk.models.*
import com.amenitypay.sdk.network.NetworkClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for authentication and customer operations
 */
class AuthRepository(
    private val sessionManager: SessionManager
) {
    
    private val apiService by lazy { NetworkClient.getApiService() }
    
    /**
     * Get customer profile
     */
    suspend fun getCustomerProfile(): Result<CustomerProfile> {
        return withContext(Dispatchers.IO) {
            try {
                val credentials = sessionManager.getCredentials()
                    ?: return@withContext Result.Error("NO_CREDENTIALS", "No credentials available")
                
                val response = apiService.getCustomerProfile(
                    customerId = credentials.customerId,
                    clientId = credentials.clientId,
                    propertyId = credentials.propertyId,
                    leaseId = credentials.leaseId
                )
                
                if (response.isSuccessful && response.body()?.success == true) {
                    val profile = response.body()?.data
                    if (profile != null) {
                        Result.Success(profile)
                    } else {
                        Result.Error("PROFILE_ERROR", "Invalid response from server")
                    }
                } else {
                    val error = response.body()?.error
                    Result.Error(
                        error?.code ?: "PROFILE_FAILED",
                        error?.message ?: "Failed to fetch profile"
                    )
                }
            } catch (e: Exception) {
                Result.Error("NETWORK_ERROR", e.message ?: "Network error occurred")
            }
        }
    }
    
    /**
     * Get current auth credentials
     */
    fun getCredentials(): AuthCredentials? = sessionManager.getCredentials()
    
    /**
     * Check if valid credentials are stored
     */
    fun hasValidCredentials(): Boolean = sessionManager.hasValidCredentials()
    
    /**
     * Clear session
     */
    fun clearSession() = sessionManager.clearSession()
}
