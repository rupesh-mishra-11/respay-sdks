package com.applepay.sdk.auth

import com.applepay.sdk.ApplePaySDK
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Thread-safe in-memory session token manager.
 * 
 * Rules:
 * - Only ONE active token may exist at a time
 * - Token is stored in memory only (no persistence)
 * - All operations are thread-safe
 * - Token is cleared after payment completion or final failure
 */
internal object SessionTokenManager {
    
    @Volatile
    private var currentToken: String? = null
    
    private val lock = ReentrantLock()
    
    /**
     * Get the current session token.
     * Returns null if no token exists.
     */
    fun getToken(): String? {
        return lock.withLock {
            currentToken
        }
    }
    
    /**
     * Set the session token.
     * Replaces any existing token (only one token allowed).
     */
    fun setToken(token: String) {
        lock.withLock {
            currentToken = token
            if (ApplePaySDK.isLoggingEnabled()) {
                android.util.Log.d("SessionTokenManager", "Token set (length: ${token.length})")
            }
        }
    }
    
    /**
     * Clear the current token.
     * Called after payment completion or final failure.
     */
    fun clearToken() {
        lock.withLock {
            val hadToken = currentToken != null
            currentToken = null
            if (hadToken && ApplePaySDK.isLoggingEnabled()) {
                android.util.Log.d("SessionTokenManager", "Token cleared")
            }
        }
    }
    
    /**
     * Check if a token exists.
     */
    fun hasToken(): Boolean {
        return lock.withLock {
            currentToken != null
        }
    }
}
