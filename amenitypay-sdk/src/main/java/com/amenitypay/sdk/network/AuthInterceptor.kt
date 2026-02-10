package com.amenitypay.sdk.network

import com.amenitypay.sdk.AmenityPaySDK
import com.amenitypay.sdk.auth.SessionManager
import okhttp3.Interceptor
import okhttp3.Response

/**
 * OkHttp interceptor that adds required headers to requests
 * - Subscription-Key for API validation
 * - Content-Type and Accept headers
 */
class AuthInterceptor(
    private val sessionManager: SessionManager
) : Interceptor {
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // Get subscription key from SDK config
        val subscriptionKey = try {
            AmenityPaySDK.getSubscriptionKey()
        } catch (e: Exception) {
            ""
        }
        
        // Build request with headers
        val authenticatedRequest = originalRequest.newBuilder()
            .header("Subscription-Key", subscriptionKey)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .build()
        
        return chain.proceed(authenticatedRequest)
    }
}
