package com.entrata.rpmobileappsdk

import android.app.Application
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.amenitypay.sdk.AmenityPaySDK
import com.amenitypay.sdk.config.SDKConfig
import com.applepay.sdk.ApplePaySDK
import com.applepay.sdk.config.ApplePayConfig
import com.payment.sdk.PaymentSDK
import com.payment.sdk.config.PaymentConfig
import com.payment.sdk.config.Environment as PaymentEnvironment
import com.paymentmethod.sdk.PaymentMethodSDK
import com.paymentmethod.sdk.config.PaymentMethodConfig

/**
 * Sample Application class demonstrating SDK initialization
 */
class SampleApplication : Application() {
    private val applePayApiKey by lazy { "Nkhb6LSJJI6wZvCb43zMQ7FfX8zLdbHE8TYRQYbo"}
    override fun onCreate() {
        super.onCreate()
        
        // Initialize shared PaymentMethod SDK first
        val paymentMethodConfig = PaymentMethodConfig.Builder()
            .apiKey("your-api-key-here")
            .environment(PaymentMethodConfig.Environment.DEVELOPMENT)
            .useMockData(true)
            .enableLogging(true)
            .build()
        
        PaymentMethodSDK.initialize(paymentMethodConfig)
        
        // Initialize AmenityPay SDK
        val amenityConfig = SDKConfig.builder()
            .baseUrl("https://api.amenitypay.com/v1/")
            .subscriptionKey("your-subscription-key-here")
            .environment(SDKConfig.Environment.DEVELOPMENT)
            .useMockData(true)
            .enableLogging(true)
            .build()
        
        AmenityPaySDK.initialize(this, amenityConfig)
        
        // Initialize Payment SDK
        val paymentConfig = PaymentConfig.builder()
            .apiKey("your-api-key-here")
            .environment(PaymentEnvironment.DEVELOPMENT)
            .useMockData(true) // Use mock data for demo
            .enableLogging(true)
            .build()
        
        PaymentSDK.initialize(this, paymentConfig)
        
        // Store API key for Apple Pay SDK (must be done before initialization)
        storeApplePayApiKey(applePayApiKey)
        
        // Initialize Apple Pay SDK
        // NOTE: If you get "UnknownHostException" but curl works in Postman:
        // 1. Try on a physical device instead of emulator (emulators often have DNS issues)
        // 2. Ensure device is on the same network as your computer
        // 3. Check Android emulator DNS settings (Settings > Network & Internet > Advanced > Private DNS)
        // 4. Try using IP address instead of hostname (if you know the IP)
        // 5. Ensure VPN is connected if required for internal hostnames
        val applePayConfig = ApplePayConfig.Builder()
            .baseUrl(AppConstants.APPLE_PAY_BFF_BASE_URL)
            .environment(ApplePayConfig.Environment.DEVELOPMENT)
            .enableLogging(true)
            .build()
        
        ApplePaySDK.initialize(
            context = this,
            config = applePayConfig,
            clientType = AppConstants.APPLE_PAY_CLIENT_TYPE
        )
    }
    
    /**
     * Store API key in EncryptedSharedPreferences for Apple Pay SDK.
     * This must be called before making any Apple Pay SDK calls.
     */
    private fun storeApplePayApiKey(apiKey: String) {
        try {
            val masterKey = MasterKey.Builder(this)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            
            val sharedPreferences = EncryptedSharedPreferences.create(
                this,
                "applepay_sdk_secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            
            sharedPreferences.edit()
                .putString("x_api_key", apiKey)
                .apply()
        } catch (e: Exception) {
            android.util.Log.e("SampleApplication", "Failed to store Apple Pay API key", e)
        }
    }
}
