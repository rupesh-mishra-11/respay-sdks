package com.amenitypay.sdk.config

/**
 * Configuration class for AmenityPay SDK
 */
data class SDKConfig private constructor(
    val baseUrl: String,
    val subscriptionKey: String,
    val environment: Environment,
    val enableLogging: Boolean,
    val connectionTimeoutSeconds: Long,
    val readTimeoutSeconds: Long,
    val useMockData: Boolean
) {
    
    enum class Environment(val displayName: String) {
        DEVELOPMENT("Development"),
        STAGING("Staging"),
        PRODUCTION("Production")
    }
    
    /**
     * Builder class for SDKConfig
     */
    class Builder {
        private var baseUrl: String = ""
        private var subscriptionKey: String = ""
        private var environment: Environment = Environment.DEVELOPMENT
        private var enableLogging: Boolean = false
        private var connectionTimeoutSeconds: Long = 30
        private var readTimeoutSeconds: Long = 30
        private var useMockData: Boolean = false
        
        /**
         * Set the base URL for API calls
         */
        fun baseUrl(url: String) = apply {
            this.baseUrl = if (url.endsWith("/")) url else "$url/"
        }
        
        /**
         * Set the subscription key for API validation
         */
        fun subscriptionKey(key: String) = apply {
            this.subscriptionKey = key
        }
        
        /**
         * Set the environment (Development, Staging, Production)
         */
        fun environment(env: Environment) = apply {
            this.environment = env
            this.enableLogging = env != Environment.PRODUCTION
        }
        
        /**
         * Enable or disable logging (disabled in production by default)
         */
        fun enableLogging(enable: Boolean) = apply {
            this.enableLogging = enable
        }
        
        /**
         * Set connection timeout in seconds
         */
        fun connectionTimeout(seconds: Long) = apply {
            this.connectionTimeoutSeconds = seconds
        }
        
        /**
         * Set read timeout in seconds
         */
        fun readTimeout(seconds: Long) = apply {
            this.readTimeoutSeconds = seconds
        }
        
        /**
         * Use mock data for testing (useful for development)
         */
        fun useMockData(useMock: Boolean) = apply {
            this.useMockData = useMock
        }
        
        /**
         * Build the SDKConfig instance
         */
        fun build(): SDKConfig {
            require(subscriptionKey.isNotBlank()) { "Subscription Key is required" }
            require(baseUrl.isNotBlank()) { "Base URL is required" }
            
            return SDKConfig(
                baseUrl = baseUrl,
                subscriptionKey = subscriptionKey,
                environment = environment,
                enableLogging = enableLogging,
                connectionTimeoutSeconds = connectionTimeoutSeconds,
                readTimeoutSeconds = readTimeoutSeconds,
                useMockData = useMockData
            )
        }
    }
    
    companion object {
        /**
         * Create a new builder instance
         */
        fun builder(): Builder = Builder()
    }
}
