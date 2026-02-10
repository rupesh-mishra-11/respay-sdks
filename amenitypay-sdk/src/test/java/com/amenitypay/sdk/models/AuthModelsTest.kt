package com.amenitypay.sdk.models

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("Auth Models")
class AuthModelsTest {

    @Nested
    @DisplayName("AuthCredentials")
    inner class AuthCredentialsTest {

        @Test
        @DisplayName("creates credentials with all fields")
        fun `creates credentials with all fields`() {
            val credentials = AuthCredentials(
                clientId = 12345,
                propertyId = 67890,
                customerId = 11111,
                leaseId = 22222
            )

            assertThat(credentials.clientId).isEqualTo(12345)
            assertThat(credentials.propertyId).isEqualTo(67890)
            assertThat(credentials.customerId).isEqualTo(11111)
            assertThat(credentials.leaseId).isEqualTo(22222)
        }

        @Test
        @DisplayName("implements equals correctly")
        fun `implements equals correctly`() {
            val credentials1 = AuthCredentials(
                clientId = 12345,
                propertyId = 67890,
                customerId = 11111,
                leaseId = 22222
            )

            val credentials2 = AuthCredentials(
                clientId = 12345,
                propertyId = 67890,
                customerId = 11111,
                leaseId = 22222
            )

            assertThat(credentials1).isEqualTo(credentials2)
        }

        @Test
        @DisplayName("copy creates new instance with updated values")
        fun `copy creates new instance with updated values`() {
            val original = AuthCredentials(
                clientId = 12345,
                propertyId = 67890,
                customerId = 11111,
                leaseId = 22222
            )

            val copied = original.copy(customerId = 99999)

            assertThat(copied.clientId).isEqualTo(12345)
            assertThat(copied.customerId).isEqualTo(99999)
            assertThat(original.customerId).isEqualTo(11111) // Original unchanged
        }
    }

    @Nested
    @DisplayName("CustomerProfile")
    inner class CustomerProfileTest {

        @Test
        @DisplayName("fullName combines first and last name")
        fun `fullName combines first and last name`() {
            val profile = CustomerProfile(
                customerId = 11111,
                clientId = 12345,
                propertyId = 67890,
                leaseId = 22222,
                firstName = "John",
                lastName = "Doe",
                email = "john@example.com",
                phone = "+1-555-123-4567"
            )

            assertThat(profile.fullName).isEqualTo("John Doe")
        }

        @Test
        @DisplayName("handles null email")
        fun `handles null email`() {
            val profile = CustomerProfile(
                customerId = 11111,
                clientId = 12345,
                propertyId = 67890,
                leaseId = 22222,
                firstName = "John",
                lastName = "Doe",
                email = null,
                phone = null
            )

            assertThat(profile.email).isNull()
            assertThat(profile.phone).isNull()
        }
    }
}
