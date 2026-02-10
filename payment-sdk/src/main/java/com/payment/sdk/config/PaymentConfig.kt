package com.payment.sdk.config

/**
 * Configuration for the Payment SDK.
 */
data class PaymentConfig private constructor(
    val apiKey: String,
    val baseUrl: String,
    val environment: Environment,
    val enableLogging: Boolean,
    val useMockData: Boolean,
    val timeoutSeconds: Int
) {
    
    class Builder {
        private var apiKey: String? = null
        private var baseUrl: String? = null
        private var environment: Environment = Environment.DEVELOPMENT
        private var enableLogging: Boolean = false
        private var useMockData: Boolean = false
        private var timeoutSeconds: Int = 30
        
        fun apiKey(key: String) = apply { this.apiKey = key }
        fun baseUrl(url: String) = apply { this.baseUrl = url }
        fun environment(env: Environment) = apply { this.environment = env }
        fun enableLogging(enable: Boolean) = apply { this.enableLogging = enable }
        fun useMockData(use: Boolean) = apply { this.useMockData = use }
        fun timeout(seconds: Int) = apply { this.timeoutSeconds = seconds }
        
        fun build(): PaymentConfig {
            val key = apiKey ?: throw IllegalArgumentException("API key is required")
            val url = baseUrl ?: environment.baseUrl
            val normalizedUrl = if (url.endsWith("/")) url else "$url/"
            
            return PaymentConfig(
                apiKey = key,
                baseUrl = normalizedUrl,
                environment = environment,
                enableLogging = enableLogging || environment != Environment.PRODUCTION,
                useMockData = useMockData,
                timeoutSeconds = timeoutSeconds
            )
        }
    }
    
    companion object {
        fun builder() = Builder()
    }
}

enum class Environment(val baseUrl: String) {
    DEVELOPMENT("https://us-residentpay-bff.d05d0001.entratadev.com/"),
    STAGING("https://staging-api.payment.com/"),
    PRODUCTION("https://api.payment.com/")
}
