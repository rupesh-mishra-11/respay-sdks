package com.applepay.sdk.repository

import com.applepay.sdk.ApplePaySDK
import com.applepay.sdk.auth.TokenExecutionWrapper
import com.applepay.sdk.models.ApplePayInitRequest
import com.applepay.sdk.models.ApplePayInitResponse
import com.applepay.sdk.models.ApplePayValidateRequest
import com.applepay.sdk.models.ApplePayValidateResponse
import com.applepay.sdk.models.ApplePayFinalizeRequest
import com.applepay.sdk.models.ApplePayFinalizeResponse
import com.applepay.sdk.network.NetworkClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.net.UnknownHostException

/**
 * Repository for Apple Pay BFF API calls.
 * 
 * Handles network requests and error mapping.
 * Protected endpoints (Validate, Finalize) use TokenExecutionWrapper for automatic token management.
 */
internal class ApplePayRepository {
    
    private val apiService = NetworkClient.getApiService()
    private var tokenWrapper: TokenExecutionWrapper? = null
    
    /**
     * Set the token execution wrapper for protected API calls.
     * Must be called before using Validate or Finalize.
     */
    fun setTokenWrapper(wrapper: TokenExecutionWrapper) {
        this.tokenWrapper = wrapper
    }
    
    /**
     * Initialize Apple Pay and check availability.
     * 
     * @param request Initialization request with session data and amount
     * @return RepositoryResult containing response or error
     */
    suspend fun initializeApplePay(request: ApplePayInitRequest): RepositoryResult<ApplePayInitResponse> {
        return withContext(Dispatchers.IO) {
            try {
                if (ApplePaySDK.isLoggingEnabled()) {
                    android.util.Log.d("ApplePayRepository", "Making request to /api/init with amount: ${request.data.amount}")
                }
                val response = apiService.initializeApplePay(request)
                if (ApplePaySDK.isLoggingEnabled()) {
                    android.util.Log.d("ApplePayRepository", "Received response with statusCode: ${response.statusCode}")
                }
                
                // Check HTTP status code
                if (response.statusCode == 200) {
                    RepositoryResult.Success(response)
                } else {
                    RepositoryResult.Error(
                        code = response.error?.code ?: "UNKNOWN_ERROR",
                        message = response.error?.message ?: "Failed to initialize Apple Pay"
                    )
                }
            } catch (e: HttpException) {
                // HTTP error (4xx, 5xx)
                RepositoryResult.Error(
                    code = "HTTP_${e.code()}",
                    message = "Network request failed: ${e.message()}"
                )
            } catch (e: UnknownHostException) {
                // DNS resolution failed
                android.util.Log.e("ApplePayRepository", "DNS resolution failed for host: ${e.message}", e)
                RepositoryResult.Error(
                    code = "NETWORK_ERROR",
                    message = "Unable to resolve host. Please check your internet connection and DNS settings. If using an emulator, try a physical device or check emulator network settings."
                )
            } catch (e: IOException) {
                // Network error (no connection, timeout, etc.)
                android.util.Log.e("ApplePayRepository", "Network I/O error: ${e.message}", e)
                RepositoryResult.Error(
                    code = "NETWORK_ERROR",
                    message = "Network connection failed. Please check your internet connection."
                )
            } catch (e: Exception) {
                // Unexpected error
                RepositoryResult.Error(
                    code = "UNEXPECTED_ERROR",
                    message = "An unexpected error occurred: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Validate Apple Pay payment data.
     * 
     * This is a protected endpoint that requires a session token.
     * Token management and retry logic is handled automatically.
     * 
     * @param request Validation request with payment data
     * @return RepositoryResult containing response or error
     */
    suspend fun validateApplePay(request: ApplePayValidateRequest): RepositoryResult<ApplePayValidateResponse> {
        val wrapper = tokenWrapper ?: throw IllegalStateException(
            "TokenExecutionWrapper not set. Call setTokenWrapper() first."
        )
        
        return wrapper.executeWithToken {
            withContext(Dispatchers.IO) {
                try {
                    val response = apiService.validateApplePay(request)
                    
                    // Check HTTP status code
                    if (response.statusCode == 200 && response.success) {
                        RepositoryResult.Success(response)
                    } else {
                        // Extract error information
                        val errorCode = response.error?.code ?: "UNKNOWN_ERROR"
                        val errorMessage = response.error?.message ?: "Validation failed"
                        
                        // Check for HTTP 401 in status code
                        val is401 = response.statusCode == 401
                        
                        RepositoryResult.Error(
                            code = if (is401) "HTTP_401" else errorCode,
                            message = errorMessage
                        )
                    }
                } catch (e: HttpException) {
                    // HTTP error (4xx, 5xx)
                    RepositoryResult.Error(
                        code = "HTTP_${e.code()}",
                        message = "Network request failed: ${e.message()}"
                    )
                } catch (e: UnknownHostException) {
                    RepositoryResult.Error(
                        code = "NETWORK_ERROR",
                        message = "Unable to resolve host. Please check your internet connection."
                    )
                } catch (e: IOException) {
                    RepositoryResult.Error(
                        code = "NETWORK_ERROR",
                        message = "Network connection failed. Please check your internet connection."
                    )
                } catch (e: Exception) {
                    RepositoryResult.Error(
                        code = "UNEXPECTED_ERROR",
                        message = "An unexpected error occurred: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * Finalize Apple Pay payment.
     * 
     * This is a protected endpoint that requires a session token.
     * Token management and retry logic is handled automatically.
     * 
     * @param request Finalization request with transaction ID
     * @return RepositoryResult containing response or error
     */
    suspend fun finalizeApplePay(request: ApplePayFinalizeRequest): RepositoryResult<ApplePayFinalizeResponse> {
        val wrapper = tokenWrapper ?: throw IllegalStateException(
            "TokenExecutionWrapper not set. Call setTokenWrapper() first."
        )
        
        return wrapper.executeWithToken {
            withContext(Dispatchers.IO) {
                try {
                    val response = apiService.finalizeApplePay(request)
                    
                    // Check HTTP status code
                    if (response.statusCode == 200 && response.success) {
                        RepositoryResult.Success(response)
                    } else {
                        // Extract error information
                        val errorCode = response.error?.code ?: "UNKNOWN_ERROR"
                        val errorMessage = response.error?.message ?: "Finalization failed"
                        
                        // Check for HTTP 401 in status code
                        val is401 = response.statusCode == 401
                        
                        RepositoryResult.Error(
                            code = if (is401) "HTTP_401" else errorCode,
                            message = errorMessage
                        )
                    }
                } catch (e: HttpException) {
                    // HTTP error (4xx, 5xx)
                    RepositoryResult.Error(
                        code = "HTTP_${e.code()}",
                        message = "Network request failed: ${e.message()}"
                    )
                } catch (e: UnknownHostException) {
                    RepositoryResult.Error(
                        code = "NETWORK_ERROR",
                        message = "Unable to resolve host. Please check your internet connection."
                    )
                } catch (e: IOException) {
                    RepositoryResult.Error(
                        code = "NETWORK_ERROR",
                        message = "Network connection failed. Please check your internet connection."
                    )
                } catch (e: Exception) {
                    RepositoryResult.Error(
                        code = "UNEXPECTED_ERROR",
                        message = "An unexpected error occurred: ${e.message}"
                    )
                }
            }
        }
    }
}

/**
 * Internal result wrapper for repository operations.
 */
internal sealed class RepositoryResult<out T> {
    data class Success<T>(val data: T) : RepositoryResult<T>()
    data class Error(val code: String, val message: String) : RepositoryResult<Nothing>()
    
    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
}
