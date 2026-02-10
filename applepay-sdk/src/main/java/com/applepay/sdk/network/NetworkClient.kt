package com.applepay.sdk.network

import android.content.Context
import com.applepay.sdk.config.ApplePayConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Network client for Apple Pay SDK.
 * 
 * Security: No hardcoded URLs or API keys.
 * API key is read from EncryptedSharedPreferences.
 */
internal object NetworkClient {
    
    private var apiService: ApiService? = null
    private var config: ApplePayConfig? = null
    private var context: Context? = null
    private var clientType: String? = null
    
    /**
     * Initialize the network client with configuration.
     * 
     * @param context Application context (for reading API key)
     * @param config SDK configuration
     * @param clientType Client type (e.g., "mobile-android", "mobile-ios")
     */
    fun initialize(
        context: Context,
        config: ApplePayConfig,
        clientType: String
    ) {
        this.context = context.applicationContext
        this.config = config
        this.clientType = clientType
        
        val clientBuilder = OkHttpClient.Builder()
            .connectTimeout(config.connectTimeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(config.readTimeoutSeconds, TimeUnit.SECONDS)
            .writeTimeout(config.readTimeoutSeconds, TimeUnit.SECONDS)
        
        // Add auth interceptor (adds x-api-key and x-client-type headers)
        context.applicationContext?.let { appContext ->
            clientType?.let { type ->
                clientBuilder.addInterceptor(AuthInterceptor(appContext, type))
            }
        }
        
        // Add logging interceptor if enabled (but never log sensitive headers or tokens)
        if (config.enableLogging) {
            val loggingInterceptor = HttpLoggingInterceptor { message ->
                // Filter out sensitive headers and tokens before logging
                val filteredMessage = message
                    .replace(Regex("x-api-key:\\s*[^\\s]+", RegexOption.IGNORE_CASE), "x-api-key: [REDACTED]")
                    .replace(Regex("x-session-token:\\s*[^\\s]+", RegexOption.IGNORE_CASE), "x-session-token: [REDACTED]")
                    .replace(Regex("\"token\"\\s*:\\s*\"[^\"]+\"", RegexOption.IGNORE_CASE), "\"token\": \"[REDACTED]\"")
                android.util.Log.d("ApplePaySDK", filteredMessage)
            }.apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            clientBuilder.addInterceptor(loggingInterceptor)
        }
        
        val okHttpClient = clientBuilder.build()
        
        val retrofit = Retrofit.Builder()
            .baseUrl(config.baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        apiService = retrofit.create(ApiService::class.java)
    }
    
    /**
     * Get the API service instance.
     */
    fun getApiService(): ApiService {
        return apiService ?: throw IllegalStateException(
            "ApplePaySDK not initialized. Call ApplePaySDK.initialize() first."
        )
    }
    
    /**
     * Check if the network client is initialized.
     */
    fun isInitialized(): Boolean = apiService != null
    
    /**
     * Reset the network client (for testing).
     */
    internal fun reset() {
        apiService = null
        config = null
        context = null
        clientType = null
    }
}
