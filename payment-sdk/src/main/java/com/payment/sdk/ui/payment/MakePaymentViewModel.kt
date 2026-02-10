package com.payment.sdk.ui.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.payment.sdk.models.AddCardRequest
import com.payment.sdk.models.FinalizeRequest
import com.payment.sdk.models.FinalizeResponse
import com.payment.sdk.models.PaymentTransaction
import com.payment.sdk.models.Result
import com.payment.sdk.models.ValidateResponse
import com.payment.sdk.repository.PaymentRepository
import com.paymentmethod.sdk.models.PaymentMethod
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Make Payment screen.
 */
class MakePaymentViewModel(
    private val paymentRepository: PaymentRepository
) : ViewModel() {
    
    // Payment methods state
    private val _paymentMethodsState = MutableStateFlow<PaymentMethodsState>(PaymentMethodsState.Loading)
    val paymentMethodsState: StateFlow<PaymentMethodsState> = _paymentMethodsState.asStateFlow()
    
    // Selected payment method
    private val _selectedMethodId = MutableStateFlow<Long?>(null)
    val selectedMethodId: StateFlow<Long?> = _selectedMethodId.asStateFlow()
    
    // Payment processing state
    private val _paymentState = MutableStateFlow<PaymentState>(PaymentState.Idle)
    val paymentState: StateFlow<PaymentState> = _paymentState.asStateFlow()
    
    // Add card state
    private val _addCardState = MutableStateFlow<AddCardState>(AddCardState.Idle)
    val addCardState: StateFlow<AddCardState> = _addCardState.asStateFlow()
    
    // Validate payment state
    private val _validateState = MutableStateFlow<ValidateState>(ValidateState.Idle)
    val validateState: StateFlow<ValidateState> = _validateState.asStateFlow()
    
    // Finalize payment state
    private val _finalizeState = MutableStateFlow<FinalizeState>(FinalizeState.Idle)
    val finalizeState: StateFlow<FinalizeState> = _finalizeState.asStateFlow()
    
    init {
        loadPaymentMethods()
    }
    
    /**
     * Load saved payment methods.
     */
    fun loadPaymentMethods() {
        viewModelScope.launch {
            _paymentMethodsState.value = PaymentMethodsState.Loading
            
            when (val result = paymentRepository.getPaymentMethods()) {
                is Result.Success -> {
                    _paymentMethodsState.value = PaymentMethodsState.Success(result.data)
                    
                    // Auto-select default method
                    if (_selectedMethodId.value == null) {
                        val defaultMethod = result.data.find { it.isDefault } ?: result.data.firstOrNull()
                        _selectedMethodId.value = defaultMethod?.id
                    }
                }
                is Result.Error -> {
                    _paymentMethodsState.value = PaymentMethodsState.Error(result.message)
                }
                is Result.Loading -> {}
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
            
            val request = AddCardRequest(
                cardNumber = cardNumber,
                expiryMonth = expiryMonth,
                expiryYear = expiryYear,
                cvv = cvv,
                cardHolderName = cardHolderName
            )
            
            when (val result = paymentRepository.addPaymentMethod(request)) {
                is Result.Success -> {
                    _addCardState.value = AddCardState.Added
                    loadPaymentMethods() // Refresh list
                    _selectedMethodId.value = result.data.id // Select new card
                }
                is Result.Error -> {
                    _addCardState.value = AddCardState.Error(result.message)
                }
                is Result.Loading -> {}
            }
        }
    }
    
    /**
     * Reset add card state.
     */
    fun resetAddCardState() {
        _addCardState.value = AddCardState.Idle
    }
    
    /**
     * Process payment.
     */
    fun processPayment(amount: Double, currency: String, description: String?) {
        val methodId = _selectedMethodId.value
        if (methodId == null) {
            _paymentState.value = PaymentState.Error("Please select a payment method")
            return
        }
        
        viewModelScope.launch {
            _paymentState.value = PaymentState.Processing
            
            when (val result = paymentRepository.processPayment(methodId, amount, currency, description)) {
                is Result.Success -> {
                    _paymentState.value = PaymentState.Success(result.data)
                }
                is Result.Error -> {
                    _paymentState.value = PaymentState.Error(result.message)
                }
                is Result.Loading -> {}
            }
        }
    }
    
    /**
     * Get selected payment method.
     */
    fun getSelectedPaymentMethod(): PaymentMethod? {
        val methods = (_paymentMethodsState.value as? PaymentMethodsState.Success)?.methods
        return methods?.find { it.id == _selectedMethodId.value }
    }
    
    /**
     * Validate payment.
     */
    fun validatePayment(
        validationUrl: String,
        cid: Long,
        propertyId: Long,
        customerId: Long,
        leaseId: Long,
        amount: Double
    ) {
        viewModelScope.launch {
            _validateState.value = ValidateState.Loading
            
            val result = paymentRepository.validatePayment(
                validationUrl = validationUrl,
                cid = cid,
                propertyId = propertyId,
                customerId = customerId,
                leaseId = leaseId,
                amount = amount
            )
            
            when (result) {
                is Result.Success -> {
                    _validateState.value = ValidateState.Success(result.data)
                }
                is Result.Error -> {
                    _validateState.value = ValidateState.Error(result.message)
                }
                is Result.Loading -> {}
            }
        }
    }
    
    /**
     * Finalize payment.
     */
    fun finalizePayment(request: FinalizeRequest) {
        viewModelScope.launch {
            _finalizeState.value = FinalizeState.Loading
            
            val result = paymentRepository.finalizePayment(request)
            
            when (result) {
                is Result.Success -> {
                    _finalizeState.value = FinalizeState.Success(result.data)
                }
                is Result.Error -> {
                    _finalizeState.value = FinalizeState.Error(result.message)
                }
                is Result.Loading -> {}
            }
        }
    }
    
    /**
     * Reset validate state.
     */
    fun resetValidateState() {
        _validateState.value = ValidateState.Idle
    }
    
    /**
     * Reset finalize state.
     */
    fun resetFinalizeState() {
        _finalizeState.value = FinalizeState.Idle
    }
}

// State classes
sealed class PaymentMethodsState {
    object Loading : PaymentMethodsState()
    data class Success(val methods: List<PaymentMethod>) : PaymentMethodsState()
    data class Error(val message: String) : PaymentMethodsState()
}

sealed class PaymentState {
    object Idle : PaymentState()
    object Processing : PaymentState()
    data class Success(val transaction: PaymentTransaction) : PaymentState()
    data class Error(val message: String) : PaymentState()
}

sealed class AddCardState {
    object Idle : AddCardState()
    object Adding : AddCardState()
    object Added : AddCardState()
    data class Error(val message: String) : AddCardState()
}

sealed class ValidateState {
    object Idle : ValidateState()
    object Loading : ValidateState()
    data class Success(val response: ValidateResponse) : ValidateState()
    data class Error(val message: String) : ValidateState()
}

sealed class FinalizeState {
    object Idle : FinalizeState()
    object Loading : FinalizeState()
    data class Success(val response: FinalizeResponse) : FinalizeState()
    data class Error(val message: String) : FinalizeState()
}

// ViewModel Factory
class MakePaymentViewModelFactory(
    private val paymentRepository: PaymentRepository
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MakePaymentViewModel(paymentRepository) as T
    }
}
