package com.amenitypay.sdk.ui.checkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.amenitypay.sdk.models.AmenityBookingRequest
import com.amenitypay.sdk.models.AmenityPayResult
import com.amenitypay.sdk.models.CustomerPaymentAccount
import com.amenitypay.sdk.models.Result
import com.amenitypay.sdk.repository.PaymentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Checkout screen
 */
class CheckoutViewModel(
    private val paymentRepository: PaymentRepository
) : ViewModel() {
    
    private val _checkoutState = MutableStateFlow<CheckoutState>(CheckoutState.Idle)
    val checkoutState: StateFlow<CheckoutState> = _checkoutState.asStateFlow()
    
    private var successResult: AmenityPayResult.Success? = null
    
    fun processPayment(
        bookingRequest: AmenityBookingRequest,
        paymentAccount: CustomerPaymentAccount
    ) {
        viewModelScope.launch {
            _checkoutState.value = CheckoutState.Processing
            
            when (val result = paymentRepository.processPayment(bookingRequest, paymentAccount)) {
                is Result.Success -> {
                    val paymentResponse = result.data
                    successResult = AmenityPayResult.Success(
                        transactionId = paymentResponse.transactionId,
                        confirmationNumber = paymentResponse.confirmationNumber,
                        amount = paymentResponse.amount,
                        currency = paymentResponse.currency,
                        amenityId = paymentResponse.amenityId
                    )
                    _checkoutState.value = CheckoutState.Success(
                        transactionId = paymentResponse.transactionId,
                        confirmationNumber = paymentResponse.confirmationNumber
                    )
                }
                is Result.Error -> {
                    _checkoutState.value = CheckoutState.Error(result.message)
                }
                is Result.Loading -> {
                    // Already processing
                }
            }
        }
    }
    
    fun getSuccessResult(): AmenityPayResult.Success? = successResult
}

/**
 * Checkout UI State
 */
sealed class CheckoutState {
    data object Idle : CheckoutState()
    data object Processing : CheckoutState()
    data class Success(
        val transactionId: String,
        val confirmationNumber: String?
    ) : CheckoutState()
    data class Error(val message: String) : CheckoutState()
}

/**
 * ViewModelFactory for CheckoutViewModel
 */
class CheckoutViewModelFactory(
    private val paymentRepository: PaymentRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CheckoutViewModel::class.java)) {
            return CheckoutViewModel(paymentRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
