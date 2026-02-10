package com.paymentmethod.sdk.network

import com.paymentmethod.sdk.config.PaymentMethodConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Network client for PaymentMethod SDK.
 */
internal object NetworkClient {
    
    private var apiService: ApiService? = null
    private var config: PaymentMethodConfig? = null
    private var authTokenProvider: (() -> String?)? = null
    
    /**
     * Initialize the network client with configuration.
     */
    fun initialize(
        config: PaymentMethodConfig,
        authTokenProvider: () -> String?
    ) {
        this.config = config
        this.authTokenProvider = authTokenProvider
        
        val clientBuilder = OkHttpClient.Builder()
            .connectTimeout(config.connectTimeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(config.readTimeoutSeconds, TimeUnit.SECONDS)
            .writeTimeout(config.readTimeoutSeconds, TimeUnit.SECONDS)
        
        // Add auth interceptor
        clientBuilder.addInterceptor(
            AuthInterceptor(
                getApiKey = { config.apiKey },
                getAuthToken = { authTokenProvider() }
            )
        )
        
        // Add mock interceptor if enabled
        if (config.useMockData) {
            clientBuilder.addInterceptor(MockInterceptor())
        }
        
        // Add logging interceptor if enabled
        if (config.enableLogging) {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
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
            "PaymentMethodSDK not initialized. Call PaymentMethodSDK.initialize() first."
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
        authTokenProvider = null
    }
}
