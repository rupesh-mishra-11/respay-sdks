package com.applepay.sdk.config

/**
 * Configuration for the ApplePay SDK.
 * 
 * Security Note: This SDK does NOT store API keys or base URLs.
 * The host app must provide these via EncryptedSharedPreferences.
 */
data class ApplePayConfig(
    val baseUrl: String,
    val environment: Environment = Environment.PRODUCTION,
    val enableLogging: Boolean = false,
    val connectTimeoutSeconds: Long = 30,
    val readTimeoutSeconds: Long = 30
) {
    
    enum class Environment {
        PRODUCTION,
        DEVELOPMENT
    }
    
    /**
     * Builder for ApplePayConfig.
     */
    class Builder {
        private var baseUrl: String = ""
        private var environment: Environment = Environment.PRODUCTION
        private var enableLogging: Boolean = false
        private var connectTimeoutSeconds: Long = 30
        private var readTimeoutSeconds: Long = 30
        
        fun baseUrl(baseUrl: String) = apply { this.baseUrl = baseUrl }
        fun environment(environment: Environment) = apply { this.environment = environment }
        fun enableLogging(enable: Boolean) = apply { this.enableLogging = enable }
        fun connectTimeout(seconds: Long) = apply { this.connectTimeoutSeconds = seconds }
        fun readTimeout(seconds: Long) = apply { this.readTimeoutSeconds = seconds }
        
        fun build(): ApplePayConfig {
            require(baseUrl.isNotBlank()) { "Base URL is required" }
            require(baseUrl.startsWith("http://") || baseUrl.startsWith("https://")) {
                "Base URL must start with http:// or https://"
            }
            // Retrofit requires base URL to end with /
            val normalizedBaseUrl = baseUrl.trimEnd('/') + "/"
            return ApplePayConfig(
                baseUrl = normalizedBaseUrl,
                environment = environment,
                enableLogging = enableLogging,
                connectTimeoutSeconds = connectTimeoutSeconds,
                readTimeoutSeconds = readTimeoutSeconds
            )
        }
    }
}

/**
 * Session data for Apple Pay initialization.
 * Set by host app after user authentication.
 */
data class SessionData(
    val cid: Int,
    val propertyId: Int,
    val customerId: Long,
    val leaseId: Long
) {
    fun validate(): Boolean {
        return cid > 0 && propertyId > 0 && customerId > 0 && leaseId > 0
    }
}
