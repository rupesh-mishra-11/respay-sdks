package com.paymentmethod.sdk.config

/**
 * Configuration for the PaymentMethod SDK.
 */
data class PaymentMethodConfig(
    val apiKey: String,
    val baseUrl: String,
    val environment: Environment = Environment.PRODUCTION,
    val enableLogging: Boolean = false,
    val useMockData: Boolean = false,
    val connectTimeoutSeconds: Long = 30,
    val readTimeoutSeconds: Long = 30
) {
    
    enum class Environment {
        PRODUCTION,
        SANDBOX,
        DEVELOPMENT
    }
    
    /**
     * Builder for PaymentMethodConfig.
     */
    class Builder {
        private var apiKey: String = ""
        private var baseUrl: String = "https://api.paymentmethod.com/"
        private var environment: Environment = Environment.PRODUCTION
        private var enableLogging: Boolean = false
        private var useMockData: Boolean = false
        private var connectTimeoutSeconds: Long = 30
        private var readTimeoutSeconds: Long = 30
        
        fun apiKey(apiKey: String) = apply { this.apiKey = apiKey }
        fun baseUrl(baseUrl: String) = apply { this.baseUrl = baseUrl }
        fun environment(environment: Environment) = apply { this.environment = environment }
        fun enableLogging(enable: Boolean) = apply { this.enableLogging = enable }
        fun useMockData(useMock: Boolean) = apply { this.useMockData = useMock }
        fun connectTimeout(seconds: Long) = apply { this.connectTimeoutSeconds = seconds }
        fun readTimeout(seconds: Long) = apply { this.readTimeoutSeconds = seconds }
        
        fun build(): PaymentMethodConfig {
            require(apiKey.isNotBlank()) { "API key is required" }
            return PaymentMethodConfig(
                apiKey = apiKey,
                baseUrl = baseUrl,
                environment = environment,
                enableLogging = enableLogging,
                useMockData = useMockData,
                connectTimeoutSeconds = connectTimeoutSeconds,
                readTimeoutSeconds = readTimeoutSeconds
            )
        }
    }
}

/**
 * Customer credentials for API authentication.
 */
data class CustomerCredentials(
    val customerId: Long,
    val authToken: String
)
