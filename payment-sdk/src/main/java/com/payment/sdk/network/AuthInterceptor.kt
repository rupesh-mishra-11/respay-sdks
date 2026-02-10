package com.payment.sdk.network

import com.applepay.sdk.ApplePaySDK
import com.payment.sdk.PaymentSDK
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Interceptor that adds authentication headers to all requests.
 * 
 * For Apple Pay validate/finalize endpoints, uses token from /api/init (via ApplePaySDK.getSessionToken()).
 * For other endpoints, uses Bearer token from PaymentSDK.
 */
class AuthInterceptor : Interceptor {
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        val config = NetworkClient.getConfig()
        val requestUrl = originalRequest.url.toString()
        
        // Log the actual URL being called
        android.util.Log.d("AuthInterceptor", "Making request to: $requestUrl")
        
        val authToken = if (isApplePayEndpoint(requestUrl)) {
            // Try to get token from Apple Pay SDK (from /api/init)
            val applePayToken = try {
                if (ApplePaySDK.isInitialized()) {
                    ApplePaySDK.getSessionToken()
                } else {
                    null
                }
            } catch (e: Exception) {
                android.util.Log.d("AuthInterceptor", "ApplePaySDK not initialized or error getting token: ${e.message}")
                null
            }
            
            // Fall back to PaymentSDK token if Apple Pay token not available
            applePayToken ?: try { PaymentSDK.getAuthToken() } catch (e: Exception) { "" }
        } else {
            try { PaymentSDK.getAuthToken() } catch (e: Exception) { "" }
        }
        
        android.util.Log.d("AuthInterceptor", "Using token for: $requestUrl (token length: ${authToken.length})")
        
        val modifiedRequest = originalRequest.newBuilder()
            .header("Authorization", "Bearer $authToken")
            .header("x-api-key", config?.apiKey ?: "")
            .header("x-client-type", "mobile-android")
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            // .header("Origin", "https://test.residentportal.lcl:3000")
            .build()
        
        val response = chain.proceed(modifiedRequest)
        
        // Log response status
        android.util.Log.d("AuthInterceptor", "Response from $requestUrl: ${response.code} ${response.message}")
        
        return response
    }
    
    /**
     * Check if the request URL is an Apple Pay endpoint (validate or finalize).
     */
    private fun isApplePayEndpoint(url: String): Boolean {
        return url.contains("/api/validate") || url.contains("/api/finalize")
    }
}
