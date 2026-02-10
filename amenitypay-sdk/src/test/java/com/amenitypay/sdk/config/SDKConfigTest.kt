package com.amenitypay.sdk.config

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("SDKConfig")
class SDKConfigTest {

    @Nested
    @DisplayName("Builder")
    inner class BuilderTest {

        @Test
        @DisplayName("throws exception when subscription key is missing")
        fun `throws exception when subscription key is missing`() {
            val exception = assertThrows<IllegalArgumentException> {
                SDKConfig.builder()
                    .baseUrl("https://api.example.com/")
                    .build()
            }

            assertThat(exception.message).contains("Subscription Key is required")
        }

        @Test
        @DisplayName("throws exception when base URL is missing")
        fun `throws exception when base URL is missing`() {
            val exception = assertThrows<IllegalArgumentException> {
                SDKConfig.builder()
                    .subscriptionKey("test-key")
                    .build()
            }

            assertThat(exception.message).contains("Base URL is required")
        }

        @Test
        @DisplayName("creates valid config with required fields")
        fun `creates valid config with required fields`() {
            val config = SDKConfig.builder()
                .baseUrl("https://api.example.com/")
                .subscriptionKey("test-subscription-key")
                .build()

            assertThat(config.baseUrl).isEqualTo("https://api.example.com/")
            assertThat(config.subscriptionKey).isEqualTo("test-subscription-key")
        }

        @Test
        @DisplayName("appends trailing slash to base URL if missing")
        fun `appends trailing slash to base URL if missing`() {
            val config = SDKConfig.builder()
                .baseUrl("https://api.example.com")
                .subscriptionKey("test-key")
                .build()

            assertThat(config.baseUrl).isEqualTo("https://api.example.com/")
        }

        @Test
        @DisplayName("does not duplicate trailing slash if present")
        fun `does not duplicate trailing slash if present`() {
            val config = SDKConfig.builder()
                .baseUrl("https://api.example.com/")
                .subscriptionKey("test-key")
                .build()

            assertThat(config.baseUrl).isEqualTo("https://api.example.com/")
        }

        @Test
        @DisplayName("sets default values correctly")
        fun `sets default values correctly`() {
            val config = SDKConfig.builder()
                .baseUrl("https://api.example.com/")
                .subscriptionKey("test-key")
                .build()

            assertThat(config.environment).isEqualTo(SDKConfig.Environment.DEVELOPMENT)
            assertThat(config.enableLogging).isFalse()
            assertThat(config.useMockData).isFalse()
            assertThat(config.connectionTimeoutSeconds).isEqualTo(30)
            assertThat(config.readTimeoutSeconds).isEqualTo(30)
        }

        @Test
        @DisplayName("enables logging automatically in development environment")
        fun `enables logging automatically in development environment`() {
            val config = SDKConfig.builder()
                .baseUrl("https://api.example.com/")
                .subscriptionKey("test-key")
                .environment(SDKConfig.Environment.DEVELOPMENT)
                .build()

            assertThat(config.enableLogging).isTrue()
        }

        @Test
        @DisplayName("enables logging automatically in staging environment")
        fun `enables logging automatically in staging environment`() {
            val config = SDKConfig.builder()
                .baseUrl("https://api.example.com/")
                .subscriptionKey("test-key")
                .environment(SDKConfig.Environment.STAGING)
                .build()

            assertThat(config.enableLogging).isTrue()
        }

        @Test
        @DisplayName("disables logging automatically in production environment")
        fun `disables logging automatically in production environment`() {
            val config = SDKConfig.builder()
                .baseUrl("https://api.example.com/")
                .subscriptionKey("test-key")
                .environment(SDKConfig.Environment.PRODUCTION)
                .build()

            assertThat(config.enableLogging).isFalse()
        }

        @Test
        @DisplayName("allows overriding logging after setting environment")
        fun `allows overriding logging after setting environment`() {
            val config = SDKConfig.builder()
                .baseUrl("https://api.example.com/")
                .subscriptionKey("test-key")
                .environment(SDKConfig.Environment.PRODUCTION)
                .enableLogging(true) // Override
                .build()

            assertThat(config.enableLogging).isTrue()
        }

        @Test
        @DisplayName("sets custom timeout values")
        fun `sets custom timeout values`() {
            val config = SDKConfig.builder()
                .baseUrl("https://api.example.com/")
                .subscriptionKey("test-key")
                .connectionTimeout(60)
                .readTimeout(90)
                .build()

            assertThat(config.connectionTimeoutSeconds).isEqualTo(60)
            assertThat(config.readTimeoutSeconds).isEqualTo(90)
        }

        @Test
        @DisplayName("sets mock data flag")
        fun `sets mock data flag`() {
            val config = SDKConfig.builder()
                .baseUrl("https://api.example.com/")
                .subscriptionKey("test-key")
                .useMockData(true)
                .build()

            assertThat(config.useMockData).isTrue()
        }
    }

    @Nested
    @DisplayName("Environment")
    inner class EnvironmentTest {

        @Test
        @DisplayName("development has correct display name")
        fun `development has correct display name`() {
            assertThat(SDKConfig.Environment.DEVELOPMENT.displayName).isEqualTo("Development")
        }

        @Test
        @DisplayName("staging has correct display name")
        fun `staging has correct display name`() {
            assertThat(SDKConfig.Environment.STAGING.displayName).isEqualTo("Staging")
        }

        @Test
        @DisplayName("production has correct display name")
        fun `production has correct display name`() {
            assertThat(SDKConfig.Environment.PRODUCTION.displayName).isEqualTo("Production")
        }
    }
}
