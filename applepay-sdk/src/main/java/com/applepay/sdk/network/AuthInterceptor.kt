package com.applepay.sdk.network

import android.content.Context
import com.applepay.sdk.auth.SessionTokenManager
import com.applepay.sdk.security.ApiKeyManager
import okhttp3.Interceptor
import okhttp3.Response

/**
 * OkHttp interceptor that adds required headers to BFF API requests.
 * 
 * Headers added:
 * - x-api-key: Read from EncryptedSharedPreferences (written by host app)
 * - x-client-type: Provided by host app during SDK initialization
 * - x-session-token: Session token for protected endpoints (Validate, Finalize)
 * - Content-Type: application/json
 * 
 * Security: Never logs sensitive headers or token values.
 */
internal class AuthInterceptor(
    private val context: Context,
    private val clientType: String
) : Interceptor {
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // Read API key from EncryptedSharedPreferences
        val apiKey = ApiKeyManager.getApiKey(context)
        
        val requestBuilder = originalRequest.newBuilder()
            .header("Content-Type", "application/json")
        
        // Add x-api-key if available
        apiKey?.let {
            requestBuilder.header("x-api-key", it)
        }
        
        // Add x-client-type (provided by host app)
        requestBuilder.header("x-client-type", clientType)
        
        // Add session token if available (for protected endpoints)
        SessionTokenManager.getToken()?.let { token ->
            requestBuilder.header("x-session-token", token)
        }
        
        return chain.proceed(requestBuilder.build())
    }
}
