package com.payment.sdk.repository

import com.google.gson.Gson
import com.payment.sdk.PaymentSDK
import com.payment.sdk.models.AddCardRequest
import com.payment.sdk.models.FinalizeRequest
import com.payment.sdk.models.FinalizeResponse
import com.payment.sdk.models.PaymentTransaction
import com.payment.sdk.models.ProcessPaymentRequest
import com.payment.sdk.models.Result
import com.payment.sdk.models.ValidateRequest
import com.payment.sdk.models.ValidateResponse
import com.payment.sdk.network.NetworkClient
import com.paymentmethod.sdk.models.PaymentMethod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.net.UnknownHostException

/**
 * Repository for payment operations.
 */
class PaymentRepository {
    
    private val apiService by lazy { NetworkClient.getApiService() }
    
    /**
     * Get saved payment methods.
     */
    suspend fun getPaymentMethods(): Result<List<PaymentMethod>> {
        return withContext(Dispatchers.IO) {
            try {
                val customerId = PaymentSDK.getCustomerId()
                val response = apiService.getPaymentMethods(customerId)
                
                if (response.isSuccessful && response.body()?.success == true) {
                    Result.Success(response.body()?.data ?: emptyList())
                } else {
                    val error = response.body()?.error
                    Result.Error(
                        error?.code ?: "FETCH_FAILED",
                        error?.message ?: "Failed to fetch payment methods"
                    )
                }
            } catch (e: Exception) {
                Result.Error("NETWORK_ERROR", e.message ?: "Network error")
            }
        }
    }
    
    /**
     * Add a new payment method.
     */
    suspend fun addPaymentMethod(request: AddCardRequest): Result<PaymentMethod> {
        return withContext(Dispatchers.IO) {
            try {
                val customerId = PaymentSDK.getCustomerId()
                val response = apiService.addPaymentMethod(customerId, request)
                
                if (response.isSuccessful && response.body()?.success == true) {
                    val method = response.body()?.data
                    if (method != null) {
                        Result.Success(method)
                    } else {
                        Result.Error("ADD_FAILED", "Invalid response")
                    }
                } else {
                    val error = response.body()?.error
                    Result.Error(
                        error?.code ?: "ADD_FAILED",
                        error?.message ?: "Failed to add card"
                    )
                }
            } catch (e: Exception) {
                Result.Error("NETWORK_ERROR", e.message ?: "Network error")
            }
        }
    }
    
    /**
     * Process payment.
     */
    suspend fun processPayment(
        paymentMethodId: Long,
        amount: Double,
        currency: String,
        description: String?
    ): Result<PaymentTransaction> {
        return withContext(Dispatchers.IO) {
            try {
                val customerId = PaymentSDK.getCustomerId()
                
                val request = ProcessPaymentRequest(
                    customerId = customerId,
                    paymentMethodId = paymentMethodId,
                    amount = amount,
                    currency = currency,
                    description = description
                )
                
                val response = apiService.processPayment(request)
                
                if (response.isSuccessful && response.body()?.success == true) {
                    val transaction = response.body()?.data
                    if (transaction != null) {
                        Result.Success(transaction)
                    } else {
                        Result.Error("PAYMENT_FAILED", "Invalid response")
                    }
                } else {
                    val error = response.body()?.error
                    Result.Error(
                        error?.code ?: "PAYMENT_FAILED",
                        error?.message ?: "Payment failed"
                    )
                }
            } catch (e: Exception) {
                Result.Error("NETWORK_ERROR", e.message ?: "Network error")
            }
        }
    }
    
    /**
     * Validate payment.
     */
    suspend fun validatePayment(
        validationUrl: String,
        cid: Long,
        propertyId: Long,
        customerId: Long,
        leaseId: Long,
        amount: Double
    ): Result<ValidateResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val request = ValidateRequest(
                    data = ValidateRequest.ValidateRequestData(
                        validationUrl = validationUrl,
                        cid = cid,
                        propertyId = propertyId,
                        customerId = customerId,
                        leaseId = leaseId,
                        amount = amount
                    )
                )
                
                val config = NetworkClient.getConfig()
                val baseUrl = config?.baseUrl ?: "NOT_SET"
                android.util.Log.d("PaymentRepository", "Validate Payment Request:")
                android.util.Log.d("PaymentRepository", "  Base URL: $baseUrl")
                android.util.Log.d("PaymentRepository", "  Endpoint: api/validate")
                android.util.Log.d("PaymentRepository", "  Full URL: ${baseUrl}api/validate")
                android.util.Log.d("PaymentRepository", "  Request Data: cid=$cid, propertyId=$propertyId, customerId=$customerId, leaseId=$leaseId, amount=$amount")
                
                val response = apiService.validatePayment(request)
                
                android.util.Log.d("PaymentRepository", "Validate Payment Response:")
                android.util.Log.d("PaymentRepository", "  isSuccessful=${response.isSuccessful}")
                android.util.Log.d("PaymentRepository", "  statusCode=${response.code()}")
                android.util.Log.d("PaymentRepository", "  message=${response.message()}")
                
                // Log full response body
                val responseBody = response.body()
                if (responseBody != null) {
                    android.util.Log.d("PaymentRepository", "=== Validate Payment Response Body ===")
                    android.util.Log.d("PaymentRepository", "  success: ${responseBody.success}")
                    android.util.Log.d("PaymentRepository", "  statusCode: ${responseBody.statusCode}")
                    android.util.Log.d("PaymentRepository", "  Transaction ID: ${responseBody.data?.transactionId}")
                    android.util.Log.d("PaymentRepository", "  Validated: ${responseBody.data?.validated}")
                    
                    // Log full response as JSON string if possible
                    try {
                        val gson = Gson()
                        val jsonResponse = gson.toJson(responseBody)
                        android.util.Log.d("PaymentRepository", "  Full Response JSON: $jsonResponse")
                    } catch (e: Exception) {
                        android.util.Log.d("PaymentRepository", "  Could not serialize response to JSON: ${e.message}")
                    }
                    
                    if (responseBody.error != null) {
                        android.util.Log.e("PaymentRepository", "  Error: code=${responseBody.error?.code}, message=${responseBody.error?.message}")
                    }
                    android.util.Log.d("PaymentRepository", "=== End Validate Response ===")
                } else {
                    android.util.Log.w("PaymentRepository", "  Response body is null")
                }
                
                // Log error body if request failed
                if (!response.isSuccessful) {
                    try {
                        val errorBody = response.errorBody()?.string()
                        android.util.Log.e("PaymentRepository", "=== Validate Payment Error Response ===")
                        android.util.Log.e("PaymentRepository", "  HTTP Status: ${response.code()} ${response.message()}")
                        android.util.Log.e("PaymentRepository", "  Error Body: $errorBody")
                        android.util.Log.e("PaymentRepository", "=== End Error Response ===")
                    } catch (e: Exception) {
                        android.util.Log.e("PaymentRepository", "  Could not read error body: ${e.message}")
                    }
                }
                
                if (response.isSuccessful && responseBody?.success == true) {
                    if (responseBody != null) {
                        android.util.Log.d("PaymentRepository", "Validate Payment Success:")
                        android.util.Log.d("PaymentRepository", "  transactionId=${responseBody.data?.transactionId}")
                        android.util.Log.d("PaymentRepository", "  validated=${responseBody.data?.validated}")
                        android.util.Log.d("PaymentRepository", "  statusCode=${responseBody.statusCode}")
                        Result.Success(responseBody)
                    } else {
                        android.util.Log.e("PaymentRepository", "Validate Payment Failed: Invalid response body")
                        Result.Error("VALIDATE_FAILED", "Invalid response")
                    }
                } else {
                    val error = responseBody?.error
                    val errorCode = error?.code ?: "VALIDATE_FAILED"
                    val errorMessage = error?.message ?: "Validation failed"
                    android.util.Log.e("PaymentRepository", "Validate Payment Error:")
                    android.util.Log.e("PaymentRepository", "  code=$errorCode")
                    android.util.Log.e("PaymentRepository", "  message=$errorMessage")
                    android.util.Log.e("PaymentRepository", "  HTTP statusCode=${response.code()}")
                    Result.Error(errorCode, errorMessage)
                }
            } catch (e: HttpException) {
                // HTTP error (4xx, 5xx)
                android.util.Log.e("PaymentRepository", "Validate Payment HTTP Exception: ${e.code()} ${e.message()}", e)
                Result.Error(
                    "HTTP_${e.code()}",
                    "Network request failed: ${e.message()}"
                )
            } catch (e: UnknownHostException) {
                // DNS resolution failed
                android.util.Log.e("PaymentRepository", "Validate Payment DNS Error: ${e.message}", e)
                Result.Error(
                    "NETWORK_ERROR",
                    "Unable to resolve host. Please check your internet connection and DNS settings."
                )
            } catch (e: IOException) {
                // Network error (no connection, timeout, etc.)
                android.util.Log.e("PaymentRepository", "Validate Payment I/O Error: ${e.message}", e)
                Result.Error(
                    "NETWORK_ERROR",
                    "Network connection failed. Please check your internet connection."
                )
            } catch (e: Exception) {
                // Unexpected error
                android.util.Log.e("PaymentRepository", "Validate Payment Exception: ${e.message}", e)
                Result.Error("UNEXPECTED_ERROR", e.message ?: "An unexpected error occurred")
            }
        }
    }
    
    /**
     * Finalize payment.
     */
    suspend fun finalizePayment(
        request: FinalizeRequest
    ): Result<FinalizeResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val config = NetworkClient.getConfig()
                val baseUrl = config?.baseUrl ?: "NOT_SET"
                android.util.Log.d("PaymentRepository", "Finalize Payment Request:")
                android.util.Log.d("PaymentRepository", "  Base URL: $baseUrl")
                android.util.Log.d("PaymentRepository", "  Endpoint: api/finalize")
                android.util.Log.d("PaymentRepository", "  Full URL: ${baseUrl}api/finalize")
                android.util.Log.d("PaymentRepository", "  Request Data: cid=${request.data.cid}, propertyId=${request.data.propertyId}, customerId=${request.data.customerId}, leaseId=${request.data.leaseId}, amount=${request.data.amount}, provider=${request.data.provider}")
                
                val response = apiService.finalizePayment(request)
                
                android.util.Log.d("PaymentRepository", "Finalize Payment Response:")
                android.util.Log.d("PaymentRepository", "  isSuccessful=${response.isSuccessful}")
                android.util.Log.d("PaymentRepository", "  statusCode=${response.code()}")
                android.util.Log.d("PaymentRepository", "  message=${response.message()}")
                
                // Log full response body
                val responseBody = response.body()
                if (responseBody != null) {
                    android.util.Log.d("PaymentRepository", "=== Finalize Payment Response Body ===")
                    android.util.Log.d("PaymentRepository", "  success: ${responseBody.success}")
                    android.util.Log.d("PaymentRepository", "  statusCode: ${responseBody.statusCode}")
                    android.util.Log.d("PaymentRepository", "  Transaction ID: ${responseBody.data?.transactionId}")
                    android.util.Log.d("PaymentRepository", "  Confirmation Number: ${responseBody.data?.confirmationNumber}")
                    android.util.Log.d("PaymentRepository", "  Amount: ${responseBody.data?.amount}")
                    android.util.Log.d("PaymentRepository", "  Currency: ${responseBody.data?.currency}")
                    
                    // Log full response as JSON string if possible
                    try {
                        val gson = Gson()
                        val jsonResponse = gson.toJson(responseBody)
                        android.util.Log.d("PaymentRepository", "  Full Response JSON: $jsonResponse")
                    } catch (e: Exception) {
                        android.util.Log.d("PaymentRepository", "  Could not serialize response to JSON: ${e.message}")
                    }
                    
                    if (responseBody.error != null) {
                        android.util.Log.e("PaymentRepository", "  Error: code=${responseBody.error?.code}, message=${responseBody.error?.message}")
                    }
                    android.util.Log.d("PaymentRepository", "=== End Finalize Response ===")
                } else {
                    android.util.Log.w("PaymentRepository", "  Response body is null")
                }
                
                // Log error body if request failed
                if (!response.isSuccessful) {
                    try {
                        val errorBody = response.errorBody()?.string()
                        android.util.Log.e("PaymentRepository", "=== Finalize Payment Error Response ===")
                        android.util.Log.e("PaymentRepository", "  HTTP Status: ${response.code()} ${response.message()}")
                        android.util.Log.e("PaymentRepository", "  Error Body: $errorBody")
                        android.util.Log.e("PaymentRepository", "=== End Error Response ===")
                    } catch (e: Exception) {
                        android.util.Log.e("PaymentRepository", "  Could not read error body: ${e.message}")
                    }
                }
                
                if (response.isSuccessful && responseBody?.success == true) {
                    if (responseBody != null) {
                        android.util.Log.d("PaymentRepository", "Finalize Payment Success:")
                        android.util.Log.d("PaymentRepository", "  transactionId=${responseBody.data?.transactionId}")
                        android.util.Log.d("PaymentRepository", "  confirmationNumber=${responseBody.data?.confirmationNumber}")
                        android.util.Log.d("PaymentRepository", "  amount=${responseBody.data?.amount}")
                        android.util.Log.d("PaymentRepository", "  currency=${responseBody.data?.currency}")
                        android.util.Log.d("PaymentRepository", "  statusCode=${responseBody.statusCode}")
                        Result.Success(responseBody)
                    } else {
                        android.util.Log.e("PaymentRepository", "Finalize Payment Failed: Invalid response body")
                        Result.Error("FINALIZE_FAILED", "Invalid response")
                    }
                } else {
                    val error = responseBody?.error
                    val errorCode = error?.code ?: "FINALIZE_FAILED"
                    val errorMessage = error?.message ?: "Finalization failed"
                    android.util.Log.e("PaymentRepository", "Finalize Payment Error:")
                    android.util.Log.e("PaymentRepository", "  code=$errorCode")
                    android.util.Log.e("PaymentRepository", "  message=$errorMessage")
                    android.util.Log.e("PaymentRepository", "  HTTP statusCode=${response.code()}")
                    Result.Error(errorCode, errorMessage)
                }
            } catch (e: HttpException) {
                // HTTP error (4xx, 5xx)
                android.util.Log.e("PaymentRepository", "Finalize Payment HTTP Exception: ${e.code()} ${e.message()}", e)
                Result.Error(
                    "HTTP_${e.code()}",
                    "Network request failed: ${e.message()}"
                )
            } catch (e: UnknownHostException) {
                // DNS resolution failed
                android.util.Log.e("PaymentRepository", "Finalize Payment DNS Error: ${e.message}", e)
                Result.Error(
                    "NETWORK_ERROR",
                    "Unable to resolve host. Please check your internet connection and DNS settings."
                )
            } catch (e: IOException) {
                // Network error (no connection, timeout, etc.)
                android.util.Log.e("PaymentRepository", "Finalize Payment I/O Error: ${e.message}", e)
                Result.Error(
                    "NETWORK_ERROR",
                    "Network connection failed. Please check your internet connection."
                )
            } catch (e: Exception) {
                // Unexpected error
                android.util.Log.e("PaymentRepository", "Finalize Payment Exception: ${e.message}", e)
                Result.Error("UNEXPECTED_ERROR", e.message ?: "An unexpected error occurred")
            }
        }
    }
}
