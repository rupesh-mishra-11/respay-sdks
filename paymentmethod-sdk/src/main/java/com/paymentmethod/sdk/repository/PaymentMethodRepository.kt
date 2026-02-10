package com.paymentmethod.sdk.repository

import com.paymentmethod.sdk.PaymentMethodSDK
import com.paymentmethod.sdk.models.*
import com.paymentmethod.sdk.network.NetworkClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for payment method operations.
 */
class PaymentMethodRepository {
    
    private val apiService by lazy { NetworkClient.getApiService() }
    
    /**
     * Get all saved payment methods for the customer.
     */
    suspend fun getPaymentMethods(): Result<List<PaymentMethod>> {
        return withContext(Dispatchers.IO) {
            try {
                val customerId = PaymentMethodSDK.getCustomerId()
                    ?: return@withContext Result.Error("NO_CUSTOMER", "Customer ID not set")
                
                val response = apiService.getPaymentMethods(customerId)
                
                if (response.isSuccessful && response.body()?.success == true) {
                    val methods = response.body()?.data?.paymentMethods ?: emptyList()
                    Result.Success(methods)
                } else {
                    val error = response.body()?.error
                    Result.Error(
                        error?.code ?: "FETCH_FAILED",
                        error?.message ?: "Failed to fetch payment methods"
                    )
                }
            } catch (e: Exception) {
                Result.Error("NETWORK_ERROR", e.message ?: "Network error occurred")
            }
        }
    }
    
    /**
     * Add a new payment method.
     */
    suspend fun addPaymentMethod(
        cardNumber: String,
        expiryMonth: Int,
        expiryYear: Int,
        cvv: String,
        cardHolderName: String,
        setAsDefault: Boolean = false
    ): Result<PaymentMethod> {
        return withContext(Dispatchers.IO) {
            try {
                val customerId = PaymentMethodSDK.getCustomerId()
                    ?: return@withContext Result.Error("NO_CUSTOMER", "Customer ID not set")
                
                val request = AddPaymentMethodRequest(
                    customerId = customerId,
                    cardNumber = cardNumber,
                    expiryMonth = expiryMonth,
                    expiryYear = expiryYear,
                    cvv = cvv,
                    cardHolderName = cardHolderName,
                    setAsDefault = setAsDefault
                )
                
                val response = apiService.addPaymentMethod(customerId, request)
                
                if (response.isSuccessful && response.body()?.success == true) {
                    val method = response.body()?.data
                    if (method != null) {
                        Result.Success(method)
                    } else {
                        Result.Error("ADD_FAILED", "Invalid response from server")
                    }
                } else {
                    val error = response.body()?.error
                    Result.Error(
                        error?.code ?: "ADD_FAILED",
                        error?.message ?: "Failed to add payment method"
                    )
                }
            } catch (e: Exception) {
                Result.Error("NETWORK_ERROR", e.message ?: "Network error occurred")
            }
        }
    }
    
    /**
     * Delete a payment method.
     */
    suspend fun deletePaymentMethod(methodId: Long): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val customerId = PaymentMethodSDK.getCustomerId()
                    ?: return@withContext Result.Error("NO_CUSTOMER", "Customer ID not set")
                
                val response = apiService.deletePaymentMethod(customerId, methodId)
                
                if (response.isSuccessful && response.body()?.success == true) {
                    Result.Success(Unit)
                } else {
                    val error = response.body()?.error
                    Result.Error(
                        error?.code ?: "DELETE_FAILED",
                        error?.message ?: "Failed to delete payment method"
                    )
                }
            } catch (e: Exception) {
                Result.Error("NETWORK_ERROR", e.message ?: "Network error occurred")
            }
        }
    }
    
    /**
     * Set a payment method as default.
     */
    suspend fun setDefaultPaymentMethod(methodId: Long): Result<PaymentMethod> {
        return withContext(Dispatchers.IO) {
            try {
                val customerId = PaymentMethodSDK.getCustomerId()
                    ?: return@withContext Result.Error("NO_CUSTOMER", "Customer ID not set")
                
                val response = apiService.setDefaultPaymentMethod(customerId, methodId)
                
                if (response.isSuccessful && response.body()?.success == true) {
                    val method = response.body()?.data
                    if (method != null) {
                        Result.Success(method)
                    } else {
                        Result.Error("SET_DEFAULT_FAILED", "Invalid response from server")
                    }
                } else {
                    val error = response.body()?.error
                    Result.Error(
                        error?.code ?: "SET_DEFAULT_FAILED",
                        error?.message ?: "Failed to set default payment method"
                    )
                }
            } catch (e: Exception) {
                Result.Error("NETWORK_ERROR", e.message ?: "Network error occurred")
            }
        }
    }
}
