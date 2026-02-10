package com.paymentmethod.sdk.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.paymentmethod.sdk.models.PaymentMethod
import com.paymentmethod.sdk.models.Result
import com.paymentmethod.sdk.repository.PaymentMethodRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the payment method selector screen.
 */
class PaymentMethodSelectorViewModel(
    private val repository: PaymentMethodRepository
) : ViewModel() {
    
    private val _paymentMethodsState = MutableStateFlow<PaymentMethodsState>(PaymentMethodsState.Loading)
    val paymentMethodsState: StateFlow<PaymentMethodsState> = _paymentMethodsState.asStateFlow()
    
    private val _selectedMethodId = MutableStateFlow<Long?>(null)
    val selectedMethodId: StateFlow<Long?> = _selectedMethodId.asStateFlow()
    
    private val _addCardState = MutableStateFlow<AddCardState>(AddCardState.Idle)
    val addCardState: StateFlow<AddCardState> = _addCardState.asStateFlow()
    
    init {
        loadPaymentMethods()
    }
    
    /**
     * Load payment methods from the repository.
     */
    fun loadPaymentMethods() {
        viewModelScope.launch {
            _paymentMethodsState.value = PaymentMethodsState.Loading
            
            when (val result = repository.getPaymentMethods()) {
                is Result.Success -> {
                    _paymentMethodsState.value = PaymentMethodsState.Success(result.data)
                    
                    // Auto-select default or first method
                    if (_selectedMethodId.value == null && result.data.isNotEmpty()) {
                        val defaultMethod = result.data.find { it.isDefault } ?: result.data.first()
                        _selectedMethodId.value = defaultMethod.id
                    }
                }
                is Result.Error -> {
                    _paymentMethodsState.value = PaymentMethodsState.Error(result.message)
                }
                is Result.Loading -> {
                    _paymentMethodsState.value = PaymentMethodsState.Loading
                }
            }
        }
    }
    
    /**
     * Select a payment method.
     */
    fun selectPaymentMethod(methodId: Long) {
        _selectedMethodId.value = methodId
    }
    
    /**
     * Get the currently selected payment method.
     */
    fun getSelectedPaymentMethod(): PaymentMethod? {
        val state = _paymentMethodsState.value
        if (state is PaymentMethodsState.Success) {
            return state.methods.find { it.id == _selectedMethodId.value }
        }
        return null
    }
    
    /**
     * Add a new payment method.
     */
    fun addPaymentMethod(
        cardNumber: String,
        expiryMonth: Int,
        expiryYear: Int,
        cvv: String,
        cardHolderName: String
    ) {
        viewModelScope.launch {
            _addCardState.value = AddCardState.Adding
            
            when (val result = repository.addPaymentMethod(
                cardNumber = cardNumber,
                expiryMonth = expiryMonth,
                expiryYear = expiryYear,
                cvv = cvv,
                cardHolderName = cardHolderName
            )) {
                is Result.Success -> {
                    _addCardState.value = AddCardState.Added(result.data)
                    // Reload payment methods to include the new one
                    loadPaymentMethods()
                    // Select the newly added card
                    _selectedMethodId.value = result.data.id
                }
                is Result.Error -> {
                    _addCardState.value = AddCardState.Error(result.message)
                }
                is Result.Loading -> {
                    _addCardState.value = AddCardState.Adding
                }
            }
        }
    }
    
    /**
     * Reset the add card state.
     */
    fun resetAddCardState() {
        _addCardState.value = AddCardState.Idle
    }
}

/**
 * State for payment methods loading.
 */
sealed class PaymentMethodsState {
    object Loading : PaymentMethodsState()
    data class Success(val methods: List<PaymentMethod>) : PaymentMethodsState()
    data class Error(val message: String) : PaymentMethodsState()
}

/**
 * State for adding a new card.
 */
sealed class AddCardState {
    object Idle : AddCardState()
    object Adding : AddCardState()
    data class Added(val paymentMethod: PaymentMethod) : AddCardState()
    data class Error(val message: String) : AddCardState()
}

/**
 * ViewModelFactory for PaymentMethodSelectorViewModel.
 */
class PaymentMethodSelectorViewModelFactory(
    private val repository: PaymentMethodRepository
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PaymentMethodSelectorViewModel::class.java)) {
            return PaymentMethodSelectorViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
