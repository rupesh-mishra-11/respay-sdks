package com.paymentmethod.sdk.network

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Interceptor for adding authentication headers to requests.
 */
class AuthInterceptor(
    private val getApiKey: () -> String,
    private val getAuthToken: () -> String?
) : Interceptor {
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        val requestBuilder = originalRequest.newBuilder()
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .header("X-API-Key", getApiKey())
        
        // Add auth token if available
        getAuthToken()?.let { token ->
            requestBuilder.header("Authorization", "Bearer $token")
        }
        
        return chain.proceed(requestBuilder.build())
    }
}
