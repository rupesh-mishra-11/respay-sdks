package com.amenitypay.sdk.repository

import com.amenitypay.sdk.auth.SessionManager
import com.amenitypay.sdk.models.AuthCredentials
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("AuthRepository")
class AuthRepositoryTest {

    private lateinit var sessionManager: SessionManager
    private lateinit var authRepository: AuthRepository

    private val mockCredentials = AuthCredentials(
        clientId = 12345,
        propertyId = 67890,
        customerId = 11111,
        leaseId = 22222
    )

    @BeforeEach
    fun setup() {
        sessionManager = mockk(relaxed = true)
        authRepository = AuthRepository(sessionManager)
    }

    @Nested
    @DisplayName("getCredentials")
    inner class GetCredentialsTest {

        @Test
        @DisplayName("returns credentials from session manager")
        fun `returns credentials from session manager`() {
            // Given
            every { sessionManager.getCredentials() } returns mockCredentials

            // When
            val result = authRepository.getCredentials()

            // Then
            assertThat(result).isEqualTo(mockCredentials)
            verify { sessionManager.getCredentials() }
        }

        @Test
        @DisplayName("returns null when no credentials stored")
        fun `returns null when no credentials stored`() {
            // Given
            every { sessionManager.getCredentials() } returns null

            // When
            val result = authRepository.getCredentials()

            // Then
            assertThat(result).isNull()
        }
    }

    @Nested
    @DisplayName("hasValidCredentials")
    inner class HasValidCredentialsTest {

        @Test
        @DisplayName("returns true when session manager has valid credentials")
        fun `returns true when session manager has valid credentials`() {
            // Given
            every { sessionManager.hasValidCredentials() } returns true

            // When
            val result = authRepository.hasValidCredentials()

            // Then
            assertThat(result).isTrue()
        }

        @Test
        @DisplayName("returns false when session manager has no credentials")
        fun `returns false when session manager has no credentials`() {
            // Given
            every { sessionManager.hasValidCredentials() } returns false

            // When
            val result = authRepository.hasValidCredentials()

            // Then
            assertThat(result).isFalse()
        }
    }

    @Nested
    @DisplayName("clearSession")
    inner class ClearSessionTest {

        @Test
        @DisplayName("delegates to session manager clearSession")
        fun `delegates to session manager clearSession`() {
            // When
            authRepository.clearSession()

            // Then
            verify { sessionManager.clearSession() }
        }
    }
}
