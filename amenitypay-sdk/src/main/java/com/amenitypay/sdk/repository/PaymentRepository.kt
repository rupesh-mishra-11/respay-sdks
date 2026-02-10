package com.amenitypay.sdk.repository

import com.amenitypay.sdk.AmenityPaySDK
import com.amenitypay.sdk.models.*
import com.amenitypay.sdk.network.NetworkClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for payment operations
 */
class PaymentRepository {
    
    private val apiService by lazy { NetworkClient.getApiService() }
    
    /**
     * Get all saved payment accounts for the customer
     */
    suspend fun getPaymentAccounts(): Result<List<CustomerPaymentAccount>> {
        return withContext(Dispatchers.IO) {
            try {
                val credentials = AmenityPaySDK.getAuthCredentials()
                    ?: return@withContext Result.Error("NO_CREDENTIALS", "No credentials available")
                
                val response = apiService.getPaymentAccounts(
                    customerId = credentials.customerId,
                    clientId = credentials.clientId,
                    propertyId = credentials.propertyId
                )
                
                if (response.isSuccessful && response.body()?.success == true) {
                    val accounts = response.body()?.data?.paymentAccounts
                    if (accounts != null) {
                        Result.Success(accounts)
                    } else {
                        Result.Success(emptyList())
                    }
                } else {
                    val error = response.body()?.error
                    Result.Error(
                        error?.code ?: "FETCH_FAILED",
                        error?.message ?: "Failed to fetch payment accounts"
                    )
                }
            } catch (e: Exception) {
                Result.Error("NETWORK_ERROR", e.message ?: "Network error occurred")
            }
        }
    }
    
    /**
     * Add a new payment account
     */
    suspend fun addPaymentAccount(request: AddPaymentAccountRequest): Result<CustomerPaymentAccount> {
        return withContext(Dispatchers.IO) {
            try {
                val credentials = AmenityPaySDK.getAuthCredentials()
                    ?: return@withContext Result.Error("NO_CREDENTIALS", "No credentials available")
                
                val response = apiService.addPaymentAccount(
                    customerId = credentials.customerId,
                    request = request
                )
                
                if (response.isSuccessful && response.body()?.success == true) {
                    val account = response.body()?.data
                    if (account != null) {
                        Result.Success(account)
                    } else {
                        Result.Error("ADD_FAILED", "Invalid response from server")
                    }
                } else {
                    val error = response.body()?.error
                    Result.Error(
                        error?.code ?: "ADD_FAILED",
                        error?.message ?: "Failed to add payment account"
                    )
                }
            } catch (e: Exception) {
                Result.Error("NETWORK_ERROR", e.message ?: "Network error occurred")
            }
        }
    }
    
    /**
     * Delete a payment account
     */
    suspend fun deletePaymentAccount(accountId: Long): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val credentials = AmenityPaySDK.getAuthCredentials()
                    ?: return@withContext Result.Error("NO_CREDENTIALS", "No credentials available")
                
                val response = apiService.deletePaymentAccount(
                    customerId = credentials.customerId,
                    accountId = accountId
                )
                
                if (response.isSuccessful && response.body()?.success == true) {
                    Result.Success(Unit)
                } else {
                    val error = response.body()?.error
                    Result.Error(
                        error?.code ?: "DELETE_FAILED",
                        error?.message ?: "Failed to delete payment account"
                    )
                }
            } catch (e: Exception) {
                Result.Error("NETWORK_ERROR", e.message ?: "Network error occurred")
            }
        }
    }
    
    /**
     * Set a payment account as default
     */
    suspend fun setDefaultPaymentAccount(accountId: Long): Result<CustomerPaymentAccount> {
        return withContext(Dispatchers.IO) {
            try {
                val credentials = AmenityPaySDK.getAuthCredentials()
                    ?: return@withContext Result.Error("NO_CREDENTIALS", "No credentials available")
                
                val response = apiService.setDefaultPaymentAccount(
                    customerId = credentials.customerId,
                    accountId = accountId
                )
                
                if (response.isSuccessful && response.body()?.success == true) {
                    val account = response.body()?.data
                    if (account != null) {
                        Result.Success(account)
                    } else {
                        Result.Error("SET_DEFAULT_FAILED", "Invalid response from server")
                    }
                } else {
                    val error = response.body()?.error
                    Result.Error(
                        error?.code ?: "SET_DEFAULT_FAILED",
                        error?.message ?: "Failed to set default payment account"
                    )
                }
            } catch (e: Exception) {
                Result.Error("NETWORK_ERROR", e.message ?: "Network error occurred")
            }
        }
    }
    
    /**
     * Process amenity payment
     */
    suspend fun processPayment(
        bookingRequest: AmenityBookingRequest,
        paymentAccount: CustomerPaymentAccount
    ): Result<PaymentResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val credentials = AmenityPaySDK.getAuthCredentials()
                    ?: return@withContext Result.Error("NO_CREDENTIALS", "No credentials available")
                
                val request = ProcessPaymentRequest(
                    clientId = credentials.clientId,
                    propertyId = credentials.propertyId,
                    customerId = credentials.customerId,
                    leaseId = credentials.leaseId,
                    amenityId = bookingRequest.amenityId,
                    startDatetime = bookingRequest.startDatetime,
                    endDatetime = bookingRequest.endDatetime,
                    customerPaymentAccountId = paymentAccount.customerPaymentAccountId,
                    paymentTypeId = paymentAccount.paymentTypeId,
                    amount = bookingRequest.amount,
                    currency = bookingRequest.currency
                )
                
                val response = apiService.processPayment(request)
                
                if (response.isSuccessful && response.body()?.success == true) {
                    val paymentResponse = response.body()?.data
                    if (paymentResponse != null) {
                        Result.Success(paymentResponse)
                    } else {
                        Result.Error("PAYMENT_FAILED", "Invalid response from server")
                    }
                } else {
                    val error = response.body()?.error
                    Result.Error(
                        error?.code ?: "PAYMENT_FAILED",
                        error?.message ?: "Payment processing failed"
                    )
                }
            } catch (e: Exception) {
                Result.Error("NETWORK_ERROR", e.message ?: "Network error occurred")
            }
        }
    }
}
