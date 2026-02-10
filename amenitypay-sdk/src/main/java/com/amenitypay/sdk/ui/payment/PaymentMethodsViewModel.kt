package com.amenitypay.sdk.ui.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.amenitypay.sdk.models.AddPaymentAccountRequest
import com.amenitypay.sdk.models.CustomerPaymentAccount
import com.amenitypay.sdk.models.CustomerProfile
import com.amenitypay.sdk.models.Result
import com.amenitypay.sdk.repository.AuthRepository
import com.amenitypay.sdk.repository.PaymentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Payment Methods screen
 */
class PaymentMethodsViewModel(
    private val authRepository: AuthRepository,
    private val paymentRepository: PaymentRepository
) : ViewModel() {
    
    private val _paymentAccountsState = MutableStateFlow<PaymentAccountsState>(PaymentAccountsState.Loading)
    val paymentAccountsState: StateFlow<PaymentAccountsState> = _paymentAccountsState.asStateFlow()
    
    private val _customerProfile = MutableStateFlow<CustomerProfile?>(null)
    val customerProfile: StateFlow<CustomerProfile?> = _customerProfile.asStateFlow()
    
    fun loadPaymentAccounts() {
        viewModelScope.launch {
            _paymentAccountsState.value = PaymentAccountsState.Loading
            
            when (val result = paymentRepository.getPaymentAccounts()) {
                is Result.Success -> {
                    _paymentAccountsState.value = PaymentAccountsState.Success(result.data)
                }
                is Result.Error -> {
                    _paymentAccountsState.value = PaymentAccountsState.Error(result.message)
                }
                is Result.Loading -> {
                    // Already loading
                }
            }
        }
    }
    
    fun loadCustomerProfile() {
        viewModelScope.launch {
            when (val result = authRepository.getCustomerProfile()) {
                is Result.Success -> {
                    _customerProfile.value = result.data
                }
                is Result.Error -> {
                    // Silently fail for profile loading
                }
                is Result.Loading -> {
                    // Loading
                }
            }
        }
    }
    
    fun addPaymentAccount(request: AddPaymentAccountRequest) {
        viewModelScope.launch {
            when (val result = paymentRepository.addPaymentAccount(request)) {
                is Result.Success -> {
                    // Reload payment accounts
                    loadPaymentAccounts()
                }
                is Result.Error -> {
                    _paymentAccountsState.value = PaymentAccountsState.Error(result.message)
                }
                is Result.Loading -> {
                    // Loading
                }
            }
        }
    }
}

/**
 * Payment Accounts UI State
 */
sealed class PaymentAccountsState {
    data object Loading : PaymentAccountsState()
    data class Success(val paymentAccounts: List<CustomerPaymentAccount>) : PaymentAccountsState()
    data class Error(val message: String) : PaymentAccountsState()
}

/**
 * ViewModelFactory for PaymentMethodsViewModel
 */
class PaymentMethodsViewModelFactory(
    private val authRepository: AuthRepository,
    private val paymentRepository: PaymentRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PaymentMethodsViewModel::class.java)) {
            return PaymentMethodsViewModel(authRepository, paymentRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
