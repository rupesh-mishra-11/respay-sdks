package com.amenitypay.sdk.network

import com.amenitypay.sdk.auth.SessionManager
import com.amenitypay.sdk.config.SDKConfig
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Network client singleton for managing API connections
 */
object NetworkClient {
    
    private var apiService: ApiService? = null
    private var retrofit: Retrofit? = null
    
    /**
     * Initialize the network client with configuration
     */
    fun initialize(config: SDKConfig, sessionManager: SessionManager) {
        val okHttpClient = buildOkHttpClient(config, sessionManager)
        val gson = GsonBuilder()
            .setLenient()
            .create()
        
        retrofit = Retrofit.Builder()
            .baseUrl(config.baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
        
        apiService = retrofit?.create(ApiService::class.java)
    }
    
    /**
     * Get the API service instance
     */
    fun getApiService(): ApiService {
        return apiService ?: throw IllegalStateException(
            "NetworkClient not initialized. Call AmenityPaySDK.initialize() first."
        )
    }
    
    /**
     * Build OkHttpClient with all interceptors
     */
    private fun buildOkHttpClient(config: SDKConfig, sessionManager: SessionManager): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(config.connectionTimeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(config.readTimeoutSeconds, TimeUnit.SECONDS)
            .writeTimeout(config.readTimeoutSeconds, TimeUnit.SECONDS)
        
        // Add auth interceptor (adds Subscription-Key header)
        builder.addInterceptor(AuthInterceptor(sessionManager))
        
        // Add mock interceptor if using mock data
        if (config.useMockData) {
            builder.addInterceptor(MockInterceptor())
        }
        
        // Add logging interceptor for debug builds
        if (config.enableLogging) {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            builder.addInterceptor(loggingInterceptor)
        }
        
        return builder.build()
    }
    
    /**
     * Clear the network client (used during SDK reset)
     */
    fun clear() {
        apiService = null
        retrofit = null
    }
}
