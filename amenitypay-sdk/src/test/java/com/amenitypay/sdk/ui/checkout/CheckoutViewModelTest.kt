package com.amenitypay.sdk.ui.checkout

import app.cash.turbine.test
import com.amenitypay.sdk.models.AmenityBookingRequest
import com.amenitypay.sdk.models.CardType
import com.amenitypay.sdk.models.CustomerPaymentAccount
import com.amenitypay.sdk.models.PaymentResponse
import com.amenitypay.sdk.models.PaymentStatus
import com.amenitypay.sdk.models.Result
import com.amenitypay.sdk.repository.PaymentRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("CheckoutViewModel")
class CheckoutViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var paymentRepository: PaymentRepository
    private lateinit var viewModel: CheckoutViewModel

    private val mockBookingRequest = AmenityBookingRequest(
        amenityId = 101,
        amenityName = "Pool Access",
        startDatetime = "2024-01-15T10:00:00Z",
        endDatetime = "2024-01-15T12:00:00Z",
        amount = 25.00,
        currency = "USD"
    )

    private val mockPaymentAccount = CustomerPaymentAccount(
        customerPaymentAccountId = 1001,
        paymentTypeId = 1,
        paymentTypeName = "Credit Card",
        cardType = CardType.VISA,
        lastFour = "4242",
        expiryMonth = 12,
        expiryYear = 2027,
        cardHolderName = "John Doe",
        isDefault = true,
        isActive = true
    )

    private val mockPaymentResponse = PaymentResponse(
        transactionId = "TXN-12345",
        status = PaymentStatus.SUCCESS,
        amount = 25.00,
        currency = "USD",
        amenityId = 101,
        customerPaymentAccountId = 1001,
        createdAt = "2024-01-15T10:30:00Z",
        confirmationNumber = "CNF-67890",
        message = "Payment successful"
    )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        paymentRepository = mockk()
        viewModel = CheckoutViewModel(paymentRepository)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Nested
    @DisplayName("processPayment")
    inner class ProcessPaymentTest {

        @Test
        @DisplayName("emits Idle then Processing then Success on successful payment")
        fun `emits Idle then Processing then Success on successful payment`() = runTest {
            // Given
            coEvery { paymentRepository.processPayment(any(), any()) } returns 
                Result.Success(mockPaymentResponse)

            // When & Then
            viewModel.checkoutState.test {
                // Initial state is Idle
                assertThat(awaitItem()).isEqualTo(CheckoutState.Idle)
                
                // Trigger payment
                viewModel.processPayment(mockBookingRequest, mockPaymentAccount)
                
                // Should emit Processing
                assertThat(awaitItem()).isEqualTo(CheckoutState.Processing)
                
                testDispatcher.scheduler.advanceUntilIdle()
                
                // Should emit Success
                val successState = awaitItem()
                assertThat(successState).isInstanceOf(CheckoutState.Success::class.java)
                assertThat((successState as CheckoutState.Success).transactionId).isEqualTo("TXN-12345")
                assertThat(successState.confirmationNumber).isEqualTo("CNF-67890")
                
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("emits Processing then Error on failed payment")
        fun `emits Processing then Error on failed payment`() = runTest {
            // Given
            coEvery { paymentRepository.processPayment(any(), any()) } returns 
                Result.Error("PAYMENT_DECLINED", "Card was declined")

            // When & Then
            viewModel.checkoutState.test {
                assertThat(awaitItem()).isEqualTo(CheckoutState.Idle)
                
                viewModel.processPayment(mockBookingRequest, mockPaymentAccount)
                
                assertThat(awaitItem()).isEqualTo(CheckoutState.Processing)
                
                testDispatcher.scheduler.advanceUntilIdle()
                
                val errorState = awaitItem()
                assertThat(errorState).isInstanceOf(CheckoutState.Error::class.java)
                assertThat((errorState as CheckoutState.Error).message).isEqualTo("Card was declined")
                
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("calls repository processPayment with correct parameters")
        fun `calls repository processPayment with correct parameters`() = runTest {
            // Given
            coEvery { paymentRepository.processPayment(any(), any()) } returns 
                Result.Success(mockPaymentResponse)

            // When
            viewModel.processPayment(mockBookingRequest, mockPaymentAccount)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            coVerify { 
                paymentRepository.processPayment(mockBookingRequest, mockPaymentAccount) 
            }
        }
    }

    @Nested
    @DisplayName("getSuccessResult")
    inner class GetSuccessResultTest {

        @Test
        @DisplayName("returns null before payment")
        fun `returns null before payment`() {
            assertThat(viewModel.getSuccessResult()).isNull()
        }

        @Test
        @DisplayName("returns result after successful payment")
        fun `returns result after successful payment`() = runTest {
            // Given
            coEvery { paymentRepository.processPayment(any(), any()) } returns 
                Result.Success(mockPaymentResponse)

            // When
            viewModel.processPayment(mockBookingRequest, mockPaymentAccount)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            val result = viewModel.getSuccessResult()
            assertThat(result).isNotNull()
            assertThat(result?.transactionId).isEqualTo("TXN-12345")
            assertThat(result?.confirmationNumber).isEqualTo("CNF-67890")
            assertThat(result?.amount).isEqualTo(25.00)
            assertThat(result?.currency).isEqualTo("USD")
            assertThat(result?.amenityId).isEqualTo(101)
        }

        @Test
        @DisplayName("returns null after failed payment")
        fun `returns null after failed payment`() = runTest {
            // Given
            coEvery { paymentRepository.processPayment(any(), any()) } returns 
                Result.Error("ERROR", "Payment failed")

            // When
            viewModel.processPayment(mockBookingRequest, mockPaymentAccount)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            assertThat(viewModel.getSuccessResult()).isNull()
        }
    }
}
