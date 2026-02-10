package com.payment.sdk.network

import com.google.gson.GsonBuilder
import com.payment.sdk.config.PaymentConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton network client manager.
 */
object NetworkClient {
    
    private var retrofit: Retrofit? = null
    private var apiService: ApiService? = null
    private var config: PaymentConfig? = null
    
    fun initialize(config: PaymentConfig) {
        this.config = config
        
        val okHttpClient = buildOkHttpClient(config)
        val gson = GsonBuilder().setLenient().create()
        
        retrofit = Retrofit.Builder()
            .baseUrl(config.baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
        
        apiService = retrofit!!.create(ApiService::class.java)
    }
    
    private fun buildOkHttpClient(config: PaymentConfig): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(config.timeoutSeconds.toLong(), TimeUnit.SECONDS)
            .readTimeout(config.timeoutSeconds.toLong(), TimeUnit.SECONDS)
            .writeTimeout(config.timeoutSeconds.toLong(), TimeUnit.SECONDS)
            .addInterceptor(AuthInterceptor())
            .apply {
                if (config.useMockData) {
                    addInterceptor(MockInterceptor())
                }
            }
            .apply {
                if (config.enableLogging) {
                    addInterceptor(HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    })
                }
            }
            .build()
    }
    
    fun getApiService(): ApiService {
        return apiService ?: throw IllegalStateException("NetworkClient not initialized")
    }
    
    fun getConfig(): PaymentConfig? = config
}
