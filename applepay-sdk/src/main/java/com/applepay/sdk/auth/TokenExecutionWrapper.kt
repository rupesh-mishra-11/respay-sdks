package com.applepay.sdk.auth

import com.applepay.sdk.ApplePaySDK
import com.applepay.sdk.models.ApplePayInitRequest
import com.applepay.sdk.models.ApplePayInitResponse
import com.applepay.sdk.repository.ApplePayRepository
import com.applepay.sdk.repository.RepositoryResult
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Central execution wrapper for protected API calls (Validate, Finalize).
 * 
 * Responsibilities:
 * - Ensure token exists (call Init if missing)
 * - Execute API call
 * - Detect invalid token errors
 * - Re-init and retry exactly once
 * - Prevent infinite retry loops
 * 
 * Thread-safe: Re-init operations are serialized to avoid race conditions.
 */
internal class TokenExecutionWrapper(
    private val repository: ApplePayRepository,
    private val getInitRequest: (Double) -> ApplePayInitRequest
) {
    
    private val reinitMutex = Mutex()
    
    @Volatile
    private var lastAmount: Double = 0.0
    
    /**
     * Execute a protected API call with automatic token management and retry.
     * 
     * @param apiCall The API call to execute (receives token as parameter)
     * @return RepositoryResult from the API call
     */
    suspend fun <T> executeWithToken(
        apiCall: suspend () -> RepositoryResult<T>
    ): RepositoryResult<T> {
        return executeWithTokenInternal(apiCall, retryCount = 0)
    }
    
    /**
     * Internal method with retry counter to prevent infinite loops.
     */
    private suspend fun <T> executeWithTokenInternal(
        apiCall: suspend () -> RepositoryResult<T>,
        retryCount: Int
    ): RepositoryResult<T> {
        // Safety check: prevent infinite retry loops
        if (retryCount > 1) {
            if (ApplePaySDK.isLoggingEnabled()) {
                android.util.Log.e("TokenExecutionWrapper", "MAX_RETRY_REACHED: Retry count ($retryCount) exceeded maximum (1). Stopping to prevent infinite loop.")
            }
            return RepositoryResult.Error(
                code = "MAX_RETRY_EXCEEDED",
                message = "Maximum retry attempts exceeded. This may indicate a persistent token issue."
            )
        }
        
        // Ensure token exists before making protected call
        val initResult = ensureTokenExists()
        if (initResult is RepositoryResult.Error) {
            if (ApplePaySDK.isLoggingEnabled()) {
                android.util.Log.e("TokenExecutionWrapper", "Failed to ensure token exists: ${initResult.message}")
            }
            return initResult
        }
        
        // Execute the API call
        val result = apiCall()
        
        // Check if result indicates invalid token
        if (isInvalidTokenError(result)) {
            // Discard current token
            SessionTokenManager.clearToken()
            
            if (ApplePaySDK.isLoggingEnabled()) {
                android.util.Log.w("TokenExecutionWrapper", "Invalid token detected (retry attempt: $retryCount), re-initializing")
            }
            
            // Re-initialize token (serialized to prevent race conditions)
            val reinitResult = reinitMutex.withLock {
                reinitializeToken()
            }
            
            if (reinitResult is RepositoryResult.Error) {
                // Re-init failed, return the original error
                if (ApplePaySDK.isLoggingEnabled()) {
                    android.util.Log.e("TokenExecutionWrapper", "Token re-initialization failed: ${reinitResult.message}")
                }
                return result
            }
            
            // Retry the API call exactly once with new token
            if (ApplePaySDK.isLoggingEnabled()) {
                android.util.Log.d("TokenExecutionWrapper", "Retrying API call with new token (retry attempt: ${retryCount + 1})")
            }
            
            val retryResult = executeWithTokenInternal(apiCall, retryCount + 1)
            
            // Check if retry also failed with invalid token (should not happen, but log it)
            if (isInvalidTokenError(retryResult)) {
                if (ApplePaySDK.isLoggingEnabled()) {
                    val errorInfo = if (retryResult is RepositoryResult.Error) {
                        "${retryResult.code} - ${retryResult.message}"
                    } else {
                        "Unknown error"
                    }
                    android.util.Log.e("TokenExecutionWrapper", "CRITICAL: Retry also returned invalid token error. This indicates a persistent token issue. Error: $errorInfo")
                }
            }
            
            return retryResult
        }
        
        return result
    }
    
    /**
     * Ensure a token exists. If not, initialize one.
     */
    private suspend fun ensureTokenExists(): RepositoryResult<ApplePayInitResponse> {
        if (SessionTokenManager.hasToken()) {
            return RepositoryResult.Success(
                ApplePayInitResponse(
                    success = true,
                    statusCode = 200,
                    data = null,
                    error = null,
                    token = SessionTokenManager.getToken()
                )
            )
        }
        
        if (ApplePaySDK.isLoggingEnabled()) {
            android.util.Log.d("TokenExecutionWrapper", "No token exists, initializing")
        }
        
        return initializeToken()
    }
    
    /**
     * Set the amount for token initialization.
     * Must be called before using executeWithToken.
     */
    fun setAmount(amount: Double) {
        lastAmount = amount
    }
    
    /**
     * Initialize a new token by calling Init API.
     */
    private suspend fun initializeToken(): RepositoryResult<ApplePayInitResponse> {
        if (lastAmount <= 0) {
            return RepositoryResult.Error(
                code = "AMOUNT_NOT_SET",
                message = "Amount not set for token initialization"
            )
        }
        
        val request = getInitRequest(lastAmount)
        val result = repository.initializeApplePay(request)
        
        when (result) {
            is RepositoryResult.Success -> {
                val token = result.data.token
                if (token != null && token.isNotBlank()) {
                    SessionTokenManager.setToken(token)
                    if (ApplePaySDK.isLoggingEnabled()) {
                        android.util.Log.d("TokenExecutionWrapper", "Token initialized successfully")
                    }
                } else {
                    return RepositoryResult.Error(
                        code = "TOKEN_MISSING",
                        message = "Init API did not return a token"
                    )
                }
            }
            is RepositoryResult.Error -> {
                if (ApplePaySDK.isLoggingEnabled()) {
                    android.util.Log.e("TokenExecutionWrapper", "Token initialization failed: ${result.message}")
                }
            }
        }
        
        return result
    }
    
    /**
     * Re-initialize token after invalidation.
     * This is serialized to prevent multiple concurrent re-inits.
     */
    private suspend fun reinitializeToken(): RepositoryResult<ApplePayInitResponse> {
        // Check if another thread already re-initialized
        if (SessionTokenManager.hasToken()) {
            return RepositoryResult.Success(
                ApplePayInitResponse(
                    success = true,
                    statusCode = 200,
                    data = null,
                    error = null,
                    token = SessionTokenManager.getToken()
                )
            )
        }
        
        return initializeToken()
    }
    
    /**
     * Check if a RepositoryResult indicates an invalid token error.
     * 
     * Invalid token conditions:
     * - HTTP status = 401
     * - error.code = 10004
     * - error.message contains "invalid", "expired", or "token not found"
     */
    private fun <T> isInvalidTokenError(result: RepositoryResult<T>): Boolean {
        if (result !is RepositoryResult.Error) {
            return false
        }
        
        // Check for HTTP 401
        if (result.code == "HTTP_401") {
            return true
        }
        
        // Check for error code 10004
        if (result.code == "10004") {
            return true
        }
        
        // Check error message for token-related keywords (case-insensitive)
        val messageLower = result.message.lowercase()
        return messageLower.contains("invalid") ||
               messageLower.contains("expired") ||
               messageLower.contains("token not found")
    }
}
