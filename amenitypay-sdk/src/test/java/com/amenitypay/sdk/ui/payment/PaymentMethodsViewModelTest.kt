package com.amenitypay.sdk.ui.payment

import app.cash.turbine.test
import com.amenitypay.sdk.models.CardType
import com.amenitypay.sdk.models.CustomerPaymentAccount
import com.amenitypay.sdk.models.CustomerProfile
import com.amenitypay.sdk.models.Result
import com.amenitypay.sdk.repository.AuthRepository
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
@DisplayName("PaymentMethodsViewModel")
class PaymentMethodsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var authRepository: AuthRepository
    private lateinit var paymentRepository: PaymentRepository
    private lateinit var viewModel: PaymentMethodsViewModel

    private val mockAccounts = listOf(
        CustomerPaymentAccount(
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
        ),
        CustomerPaymentAccount(
            customerPaymentAccountId = 1002,
            paymentTypeId = 1,
            paymentTypeName = "Credit Card",
            cardType = CardType.MASTERCARD,
            lastFour = "5555",
            expiryMonth = 6,
            expiryYear = 2026,
            cardHolderName = "John Doe",
            isDefault = false,
            isActive = true
        )
    )

    private val mockProfile = CustomerProfile(
        customerId = 11111,
        clientId = 12345,
        propertyId = 67890,
        leaseId = 22222,
        firstName = "John",
        lastName = "Doe",
        email = "john@example.com",
        phone = "+1-555-123-4567"
    )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        authRepository = mockk()
        paymentRepository = mockk()
        viewModel = PaymentMethodsViewModel(authRepository, paymentRepository)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Nested
    @DisplayName("loadPaymentAccounts")
    inner class LoadPaymentAccountsTest {

        @Test
        @DisplayName("emits Loading then Success when repository returns data")
        fun `emits Loading then Success when repository returns data`() = runTest {
            // Given
            coEvery { paymentRepository.getPaymentAccounts() } returns Result.Success(mockAccounts)

            // When & Then
            viewModel.paymentAccountsState.test {
                // Initial state is Loading
                assertThat(awaitItem()).isEqualTo(PaymentAccountsState.Loading)
                
                // Trigger load
                viewModel.loadPaymentAccounts()
                testDispatcher.scheduler.advanceUntilIdle()
                
                // Should emit Success
                val successState = awaitItem()
                assertThat(successState).isInstanceOf(PaymentAccountsState.Success::class.java)
                assertThat((successState as PaymentAccountsState.Success).paymentAccounts).hasSize(2)
                
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("emits Loading then Error when repository returns error")
        fun `emits Loading then Error when repository returns error`() = runTest {
            // Given
            coEvery { paymentRepository.getPaymentAccounts() } returns 
                Result.Error("NETWORK_ERROR", "Network error occurred")

            // When & Then
            viewModel.paymentAccountsState.test {
                assertThat(awaitItem()).isEqualTo(PaymentAccountsState.Loading)
                
                viewModel.loadPaymentAccounts()
                testDispatcher.scheduler.advanceUntilIdle()
                
                val errorState = awaitItem()
                assertThat(errorState).isInstanceOf(PaymentAccountsState.Error::class.java)
                assertThat((errorState as PaymentAccountsState.Error).message).isEqualTo("Network error occurred")
                
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("calls repository getPaymentAccounts")
        fun `calls repository getPaymentAccounts`() = runTest {
            // Given
            coEvery { paymentRepository.getPaymentAccounts() } returns Result.Success(emptyList())

            // When
            viewModel.loadPaymentAccounts()
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            coVerify { paymentRepository.getPaymentAccounts() }
        }
    }

    @Nested
    @DisplayName("loadCustomerProfile")
    inner class LoadCustomerProfileTest {

        @Test
        @DisplayName("updates customerProfile when repository returns data")
        fun `updates customerProfile when repository returns data`() = runTest {
            // Given
            coEvery { authRepository.getCustomerProfile() } returns Result.Success(mockProfile)

            // When & Then
            viewModel.customerProfile.test {
                // Initial state is null
                assertThat(awaitItem()).isNull()
                
                viewModel.loadCustomerProfile()
                testDispatcher.scheduler.advanceUntilIdle()
                
                val profile = awaitItem()
                assertThat(profile).isNotNull()
                assertThat(profile?.fullName).isEqualTo("John Doe")
                assertThat(profile?.email).isEqualTo("john@example.com")
                
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("keeps null when repository returns error")
        fun `keeps null when repository returns error`() = runTest {
            // Given
            coEvery { authRepository.getCustomerProfile() } returns 
                Result.Error("ERROR", "Failed to load profile")

            // When
            viewModel.loadCustomerProfile()
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            assertThat(viewModel.customerProfile.value).isNull()
        }
    }

    @Nested
    @DisplayName("addPaymentAccount")
    inner class AddPaymentAccountTest {

        @Test
        @DisplayName("reloads payment accounts after successful add")
        fun `reloads payment accounts after successful add`() = runTest {
            // Given
            val newAccount = mockAccounts.first()
            coEvery { paymentRepository.addPaymentAccount(any()) } returns Result.Success(newAccount)
            coEvery { paymentRepository.getPaymentAccounts() } returns Result.Success(mockAccounts)

            // When
            viewModel.addPaymentAccount(mockk(relaxed = true))
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            coVerify { paymentRepository.addPaymentAccount(any()) }
            coVerify { paymentRepository.getPaymentAccounts() }
        }
    }
}
