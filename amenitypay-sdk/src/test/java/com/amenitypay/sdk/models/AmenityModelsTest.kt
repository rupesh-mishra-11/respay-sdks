package com.amenitypay.sdk.models

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("Amenity Models")
class AmenityModelsTest {

    @Nested
    @DisplayName("AmenityBookingRequest")
    inner class AmenityBookingRequestTest {

        @Test
        @DisplayName("creates booking request with all fields")
        fun `creates booking request with all fields`() {
            val request = AmenityBookingRequest(
                amenityId = 101,
                amenityName = "Pool Access",
                startDatetime = "2024-01-15T10:00:00Z",
                endDatetime = "2024-01-15T12:00:00Z",
                amount = 25.00,
                currency = "USD",
                description = "Pool reservation"
            )

            assertThat(request.amenityId).isEqualTo(101)
            assertThat(request.amenityName).isEqualTo("Pool Access")
            assertThat(request.startDatetime).isEqualTo("2024-01-15T10:00:00Z")
            assertThat(request.endDatetime).isEqualTo("2024-01-15T12:00:00Z")
            assertThat(request.amount).isEqualTo(25.00)
            assertThat(request.currency).isEqualTo("USD")
            assertThat(request.description).isEqualTo("Pool reservation")
        }

        @Test
        @DisplayName("uses default currency USD")
        fun `uses default currency USD`() {
            val request = AmenityBookingRequest(
                amenityId = 101,
                amenityName = "Pool Access",
                startDatetime = "2024-01-15T10:00:00Z",
                endDatetime = "2024-01-15T12:00:00Z",
                amount = 25.00
            )

            assertThat(request.currency).isEqualTo("USD")
        }

        @Test
        @DisplayName("allows null description")
        fun `allows null description`() {
            val request = AmenityBookingRequest(
                amenityId = 101,
                amenityName = "Pool Access",
                startDatetime = "2024-01-15T10:00:00Z",
                endDatetime = "2024-01-15T12:00:00Z",
                amount = 25.00
            )

            assertThat(request.description).isNull()
        }
    }

    @Nested
    @DisplayName("AmenityPayResult")
    inner class AmenityPayResultTest {

        @Test
        @DisplayName("Success contains all payment details")
        fun `Success contains all payment details`() {
            val result = AmenityPayResult.Success(
                transactionId = "TXN-12345",
                confirmationNumber = "CNF-67890",
                amount = 50.00,
                currency = "USD",
                amenityId = 101
            )

            assertThat(result.transactionId).isEqualTo("TXN-12345")
            assertThat(result.confirmationNumber).isEqualTo("CNF-67890")
            assertThat(result.amount).isEqualTo(50.00)
            assertThat(result.currency).isEqualTo("USD")
            assertThat(result.amenityId).isEqualTo(101)
        }

        @Test
        @DisplayName("Success allows null confirmation number")
        fun `Success allows null confirmation number`() {
            val result = AmenityPayResult.Success(
                transactionId = "TXN-12345",
                confirmationNumber = null,
                amount = 50.00,
                currency = "USD",
                amenityId = 101
            )

            assertThat(result.confirmationNumber).isNull()
        }

        @Test
        @DisplayName("Failure contains error details")
        fun `Failure contains error details`() {
            val result = AmenityPayResult.Failure(
                errorCode = "PAYMENT_DECLINED",
                errorMessage = "Card was declined"
            )

            assertThat(result.errorCode).isEqualTo("PAYMENT_DECLINED")
            assertThat(result.errorMessage).isEqualTo("Card was declined")
        }

        @Test
        @DisplayName("Cancelled is singleton object")
        fun `Cancelled is singleton object`() {
            val result1 = AmenityPayResult.Cancelled
            val result2 = AmenityPayResult.Cancelled

            assertThat(result1).isSameInstanceAs(result2)
        }

        @Test
        @DisplayName("sealed class types are correctly identified")
        fun `sealed class types are correctly identified`() {
            val success: AmenityPayResult = AmenityPayResult.Success(
                transactionId = "TXN-12345",
                confirmationNumber = null,
                amount = 50.00,
                currency = "USD",
                amenityId = 101
            )
            val failure: AmenityPayResult = AmenityPayResult.Failure(
                errorCode = "ERROR",
                errorMessage = "Error"
            )
            val cancelled: AmenityPayResult = AmenityPayResult.Cancelled

            assertThat(success).isInstanceOf(AmenityPayResult.Success::class.java)
            assertThat(failure).isInstanceOf(AmenityPayResult.Failure::class.java)
            assertThat(cancelled).isInstanceOf(AmenityPayResult.Cancelled::class.java)
        }
    }

    @Nested
    @DisplayName("ProcessPaymentRequest")
    inner class ProcessPaymentRequestTest {

        @Test
        @DisplayName("creates request with all required fields")
        fun `creates request with all required fields`() {
            val request = ProcessPaymentRequest(
                clientId = 12345,
                propertyId = 67890,
                customerId = 11111,
                leaseId = 22222,
                amenityId = 101,
                startDatetime = "2024-01-15T10:00:00Z",
                endDatetime = "2024-01-15T12:00:00Z",
                customerPaymentAccountId = 1001,
                paymentTypeId = 1,
                amount = 50.00,
                currency = "USD"
            )

            assertThat(request.clientId).isEqualTo(12345)
            assertThat(request.propertyId).isEqualTo(67890)
            assertThat(request.customerId).isEqualTo(11111)
            assertThat(request.leaseId).isEqualTo(22222)
            assertThat(request.amenityId).isEqualTo(101)
            assertThat(request.customerPaymentAccountId).isEqualTo(1001)
            assertThat(request.paymentTypeId).isEqualTo(1)
            assertThat(request.amount).isEqualTo(50.00)
            assertThat(request.currency).isEqualTo("USD")
        }
    }
}
