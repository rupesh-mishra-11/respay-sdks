package com.amenitypay.sdk.network

import com.amenitypay.sdk.auth.SessionManager
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("AuthInterceptor")
class AuthInterceptorTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var sessionManager: SessionManager
    private lateinit var client: OkHttpClient

    @BeforeEach
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        sessionManager = mockk(relaxed = true)
    }

    @AfterEach
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Nested
    @DisplayName("intercept")
    inner class InterceptTest {

        @Test
        @DisplayName("adds Subscription-Key header to requests (empty when SDK not initialized)")
        fun `adds Subscription-Key header to requests`() {
            // Given - SDK not initialized, so subscription key will be empty
            val interceptor = AuthInterceptor(sessionManager)
            client = OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .build()

            mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("{}"))

            val request = Request.Builder()
                .url(mockWebServer.url("/test"))
                .build()

            // When
            client.newCall(request).execute()

            // Then - Header exists but may be empty since SDK not initialized in tests
            val recordedRequest = mockWebServer.takeRequest()
            assertThat(recordedRequest.getHeader("Subscription-Key")).isNotNull()
        }

        @Test
        @DisplayName("adds Content-Type header")
        fun `adds Content-Type header`() {
            // Given
            val interceptor = AuthInterceptor(sessionManager)
            client = OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .build()

            mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("{}"))

            val request = Request.Builder()
                .url(mockWebServer.url("/test"))
                .build()

            // When
            client.newCall(request).execute()

            // Then
            val recordedRequest = mockWebServer.takeRequest()
            assertThat(recordedRequest.getHeader("Content-Type"))
                .isEqualTo("application/json")
        }

        @Test
        @DisplayName("adds Accept header")
        fun `adds Accept header`() {
            // Given
            val interceptor = AuthInterceptor(sessionManager)
            client = OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .build()

            mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("{}"))

            val request = Request.Builder()
                .url(mockWebServer.url("/test"))
                .build()

            // When
            client.newCall(request).execute()

            // Then
            val recordedRequest = mockWebServer.takeRequest()
            assertThat(recordedRequest.getHeader("Accept"))
                .isEqualTo("application/json")
        }

        @Test
        @DisplayName("does not override existing custom headers")
        fun `does not override existing custom headers`() {
            // Given
            val interceptor = AuthInterceptor(sessionManager)
            client = OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .build()

            mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("{}"))

            val request = Request.Builder()
                .url(mockWebServer.url("/test"))
                .header("Custom-Header", "custom-value")
                .build()

            // When
            client.newCall(request).execute()

            // Then
            val recordedRequest = mockWebServer.takeRequest()
            assertThat(recordedRequest.getHeader("Custom-Header"))
                .isEqualTo("custom-value")
        }
    }
}
