package com.amenitypay.sdk.models

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource

@DisplayName("Payment Models")
class PaymentModelsTest {

    @Nested
    @DisplayName("CustomerPaymentAccount")
    inner class CustomerPaymentAccountTest {

        private fun createAccount(
            lastFour: String = "4242",
            expiryMonth: Int = 12,
            expiryYear: Int = 2027,
            cardType: CardType = CardType.VISA,
            isDefault: Boolean = false
        ) = CustomerPaymentAccount(
            customerPaymentAccountId = 1001,
            paymentTypeId = 1,
            paymentTypeName = "Credit Card",
            cardType = cardType,
            lastFour = lastFour,
            expiryMonth = expiryMonth,
            expiryYear = expiryYear,
            cardHolderName = "John Doe",
            isDefault = isDefault,
            isActive = true
        )

        @Test
        @DisplayName("maskedNumber formats correctly")
        fun `maskedNumber formats correctly`() {
            val account = createAccount(lastFour = "4242")
            
            assertThat(account.maskedNumber).isEqualTo("•••• •••• •••• 4242")
        }

        @ParameterizedTest
        @CsvSource(
            "1, 2027, 01/27",
            "12, 2027, 12/27",
            "6, 2030, 06/30",
            "10, 2025, 10/25"
        )
        @DisplayName("expiryDate formats correctly")
        fun `expiryDate formats correctly`(month: Int, year: Int, expected: String) {
            val account = createAccount(expiryMonth = month, expiryYear = year)
            
            assertThat(account.expiryDate).isEqualTo(expected)
        }

        @Test
        @DisplayName("isExpired returns false for future date")
        fun `isExpired returns false for future date`() {
            val account = createAccount(expiryMonth = 12, expiryYear = 2030)
            
            assertThat(account.isExpired).isFalse()
        }

        @Test
        @DisplayName("isExpired returns true for past date")
        fun `isExpired returns true for past date`() {
            val account = createAccount(expiryMonth = 1, expiryYear = 2020)
            
            assertThat(account.isExpired).isTrue()
        }
    }

    @Nested
    @DisplayName("CardType")
    inner class CardTypeTest {

        @ParameterizedTest
        @EnumSource(CardType::class)
        @DisplayName("all card types have display names")
        fun `all card types have display names`(cardType: CardType) {
            assertThat(cardType.displayName).isNotEmpty()
        }

        @Test
        @DisplayName("VISA has correct display name")
        fun `VISA has correct display name`() {
            assertThat(CardType.VISA.displayName).isEqualTo("Visa")
        }

        @Test
        @DisplayName("MASTERCARD has correct display name")
        fun `MASTERCARD has correct display name`() {
            assertThat(CardType.MASTERCARD.displayName).isEqualTo("Mastercard")
        }

        @Test
        @DisplayName("AMEX has correct display name")
        fun `AMEX has correct display name`() {
            assertThat(CardType.AMEX.displayName).isEqualTo("American Express")
        }

        @Test
        @DisplayName("DISCOVER has correct display name")
        fun `DISCOVER has correct display name`() {
            assertThat(CardType.DISCOVER.displayName).isEqualTo("Discover")
        }

        @Test
        @DisplayName("UNKNOWN has correct display name")
        fun `UNKNOWN has correct display name`() {
            assertThat(CardType.UNKNOWN.displayName).isEqualTo("Card")
        }
    }

    @Nested
    @DisplayName("PaymentStatus")
    inner class PaymentStatusTest {

        @ParameterizedTest
        @EnumSource(PaymentStatus::class)
        @DisplayName("all statuses have display names")
        fun `all statuses have display names`(status: PaymentStatus) {
            assertThat(status.displayName).isNotEmpty()
        }

        @Test
        @DisplayName("SUCCESS has correct display name")
        fun `SUCCESS has correct display name`() {
            assertThat(PaymentStatus.SUCCESS.displayName).isEqualTo("Successful")
        }

        @Test
        @DisplayName("FAILED has correct display name")
        fun `FAILED has correct display name`() {
            assertThat(PaymentStatus.FAILED.displayName).isEqualTo("Failed")
        }
    }
}
