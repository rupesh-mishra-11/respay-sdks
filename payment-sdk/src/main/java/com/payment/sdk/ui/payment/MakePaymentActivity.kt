package com.payment.sdk.ui.payment

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.applepay.sdk.ApplePayIntegrationHelper
import com.applepay.sdk.ApplePaySDK
import com.applepay.sdk.callback.ApplePayAvailabilityCallback
import com.applepay.sdk.callback.ApplePayCallback
import com.applepay.sdk.models.ApplePayWalletInfo
import com.payment.sdk.PaymentSDK
import com.payment.sdk.models.FinalizeRequest
import com.payment.sdk.models.FinalizeResponse
import com.payment.sdk.models.ValidateResponse
import com.payment.sdk.R
import com.payment.sdk.databinding.PsdkActivityMakePaymentBinding
import com.payment.sdk.databinding.PsdkDialogAddCardBinding
import com.payment.sdk.models.PaymentRequest
import com.payment.sdk.models.PaymentResult
import com.paymentmethod.sdk.ui.PaymentMethodAdapter
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

/**
 * Activity for making a payment with saved or new payment methods.
 */
class MakePaymentActivity : AppCompatActivity() {
    
    private lateinit var binding: PsdkActivityMakePaymentBinding
    private lateinit var viewModel: MakePaymentViewModel
    private lateinit var adapter: PaymentMethodAdapter
    
    private var paymentRequest: PaymentRequest? = null
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US)
    private var validatedTransactionId: String? = null
    private var validateResponse: ValidateResponse? = null
    private var isApplePayLoading = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = PsdkActivityMakePaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        @Suppress("DEPRECATION")
        paymentRequest = intent.getSerializableExtra(PaymentSDK.EXTRA_PAYMENT_REQUEST) as? PaymentRequest
        
        if (paymentRequest == null) {
            // Post finish to next frame to avoid transition animation issues
            binding.root.post {
                finishWithError("INVALID_REQUEST", "Payment request is required")
            }
            return
        }
        
        setupViewModel()
        setupViews()
        observeState()
    }
    
    private fun setupViewModel() {
        viewModel = ViewModelProvider(
            this,
            MakePaymentViewModelFactory(PaymentSDK.getPaymentRepository())
        )[MakePaymentViewModel::class.java]
    }
    
    private fun setupViews() {
        // Display payment amount
        binding.psdkTvAmount.text = currencyFormatter.format(paymentRequest?.amount ?: 0.0)
        binding.psdkTvDescription.text = paymentRequest?.description ?: "Payment"
        binding.psdkTvDescription.visibility = if (paymentRequest?.description != null) View.VISIBLE else View.GONE
        
        // Toolbar
        binding.psdkBtnBack.setOnClickListener {
            finishWithResult(PaymentResult.Cancelled)
        }
        
        binding.psdkBtnClose.setOnClickListener {
            finishWithResult(PaymentResult.Cancelled)
        }
        
        // Payment methods list
        adapter = PaymentMethodAdapter { method ->
            viewModel.selectPaymentMethod(method.id)
        }
        
        binding.psdkRvPaymentMethods.apply {
            layoutManager = LinearLayoutManager(this@MakePaymentActivity)
            adapter = this@MakePaymentActivity.adapter
        }
        
        // Add card button
        binding.psdkBtnAddCard.setOnClickListener {
            showAddCardDialog()
        }
        
        // Validate button
        binding.psdkBtnValidate.setOnClickListener {
            callValidateApi()
        }
        
        // Finalise button
        binding.psdkBtnFinalise.setOnClickListener {
            callFinaliseApi()
        }
        
        // Pay button
        binding.psdkBtnPay.setOnClickListener {
            processPayment()
        }
        
        // Retry button
        binding.psdkBtnRetry.setOnClickListener {
            viewModel.loadPaymentMethods()
        }
        
        // Setup Apple Pay option
        setupApplePay()
    }
    
    private fun setupApplePay() {
        // Verify Apple Pay SDK is initialized
        if (!ApplePaySDK.isInitialized()) {
            binding.psdkRadioApplePay.visibility = View.GONE
            return
        }
        
        val amount = paymentRequest?.amount ?: 0.0
        if (amount <= 0) {
            binding.psdkRadioApplePay.visibility = View.GONE
            return
        }
        
        // Setup click handler - when radio button is clicked, check availability first, then start Apple Pay
        ApplePayIntegrationHelper.setupApplePayClickHandler(
            activity = this,
            amount = amount,
            view = binding.psdkRadioApplePay,
            callback = object : ApplePayCallback {
                override fun onInitSuccess() {
                    // Apple Pay initialized successfully
                    // Token is stored internally by SDK, proceed with payment flow
                    // Stay on the same screen - user can proceed with Apple Pay payment
                    isApplePayLoading = false
                    hideLoader()
                    Toast.makeText(
                        this@MakePaymentActivity,
                        "Apple Pay initialized successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    // TODO: Integrate Apple Pay payment processing here
                    // After getting payment data from Apple Pay, call:
                    // ApplePaySDK.validateApplePay(paymentData, validateCallback)
                    // Then ApplePaySDK.finalizeApplePay(transactionId, finalizeCallback)
                    // Activity stays open for user to continue with payment
                }
                
                override fun onFailure(errorCode: String, message: String) {
                    isApplePayLoading = false
                    hideLoader()
                    val userMessage = when (errorCode) {
                        "NETWORK_ERROR" -> {
                            "Unable to connect to Apple Pay service. Please check your internet connection or contact support if the issue persists."
                        }
                        "APPLE_PAY_UNAVAILABLE" -> {
                            "Apple Pay is not available for this payment."
                        }
                        else -> {
                            "Apple Pay failed: $message"
                        }
                    }
                    
                    Toast.makeText(
                        this@MakePaymentActivity,
                        userMessage,
                        Toast.LENGTH_LONG
                    ).show()
                    
                    android.util.Log.e("MakePaymentActivity", "Apple Pay error: $errorCode - $message")
                }
                
                override fun onCancelled() {
                    isApplePayLoading = false
                    hideLoader()
                    // User cancelled Apple Pay
                }
            },
            availabilityCallback = object : ApplePayAvailabilityCallback {
                override fun onAvailable(walletInfo: ApplePayWalletInfo) {
                    android.util.Log.d("MakePaymentActivity", "Apple Pay is available")
                }
                
                override fun onUnavailable(message: String) {
                    isApplePayLoading = false
                    hideLoader()
                    android.util.Log.w("MakePaymentActivity", "Apple Pay unavailable: $message")
                }
                
                override fun onError(errorCode: String, message: String) {
                    isApplePayLoading = false
                    hideLoader()
                    android.util.Log.e("MakePaymentActivity", "Apple Pay error: $errorCode - $message")
                    // Show user-friendly error message
                    val userMessage = when (errorCode) {
                        "NETWORK_ERROR" -> {
                            "Unable to connect to Apple Pay service. Please check your internet connection."
                        }
                        else -> {
                            "Apple Pay error: $message"
                        }
                    }
                    Toast.makeText(
                        this@MakePaymentActivity,
                        userMessage,
                        Toast.LENGTH_LONG
                    ).show()
                }
            },
            onLoadingStart = {
                isApplePayLoading = true
                showLoader()
            }
        )
    }
    
    private fun observeState() {
        // Observe payment methods
        lifecycleScope.launch {
            viewModel.paymentMethodsState.collect { state ->
                when (state) {
                    is PaymentMethodsState.Loading -> {
                        binding.psdkProgressBar.visibility = View.VISIBLE
                        binding.psdkContentLayout.visibility = View.GONE
                        binding.psdkErrorLayout.visibility = View.GONE
                    }
                    is PaymentMethodsState.Success -> {
                        binding.psdkProgressBar.visibility = View.GONE
                        binding.psdkContentLayout.visibility = View.VISIBLE
                        binding.psdkErrorLayout.visibility = View.GONE
                        
                        if (state.methods.isEmpty()) {
                            binding.psdkRvPaymentMethods.visibility = View.GONE
                            binding.psdkEmptyState.visibility = View.VISIBLE
                        } else {
                            binding.psdkRvPaymentMethods.visibility = View.VISIBLE
                            binding.psdkEmptyState.visibility = View.GONE
                            adapter.submitList(state.methods)
                        }
                    }
                    is PaymentMethodsState.Error -> {
                        binding.psdkProgressBar.visibility = View.GONE
                        binding.psdkContentLayout.visibility = View.GONE
                        binding.psdkErrorLayout.visibility = View.VISIBLE
                        binding.psdkTvErrorMessage.text = state.message
                    }
                }
            }
        }
        
        // Observe selected method
        lifecycleScope.launch {
            viewModel.selectedMethodId.collect { methodId ->
                adapter.setSelectedId(methodId)
                binding.psdkBtnPay.isEnabled = methodId != null
            }
        }
        
        // Observe payment state
        lifecycleScope.launch {
            viewModel.paymentState.collect { state ->
                when (state) {
                    is PaymentState.Idle -> {
                        binding.psdkBtnPay.isEnabled = viewModel.selectedMethodId.value != null
                        binding.psdkPaymentProgress.visibility = View.GONE
                    }
                    is PaymentState.Processing -> {
                        binding.psdkBtnPay.isEnabled = false
                        binding.psdkPaymentProgress.visibility = View.VISIBLE
                    }
                    is PaymentState.Success -> {
                        binding.psdkPaymentProgress.visibility = View.GONE
                        showSuccessAndFinish(state.transaction)
                    }
                    is PaymentState.Error -> {
                        binding.psdkBtnPay.isEnabled = true
                        binding.psdkPaymentProgress.visibility = View.GONE
                        Toast.makeText(this@MakePaymentActivity, state.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
        
        // Observe add card state
        lifecycleScope.launch {
            viewModel.addCardState.collect { state ->
                when (state) {
                    is AddCardState.Adding -> {
                        showLoader()
                    }
                    is AddCardState.Added -> {
                        hideLoader()
                        Toast.makeText(this@MakePaymentActivity, R.string.card_added, Toast.LENGTH_SHORT).show()
                        viewModel.resetAddCardState()
                    }
                    is AddCardState.Error -> {
                        hideLoader()
                        Toast.makeText(this@MakePaymentActivity, state.message, Toast.LENGTH_LONG).show()
                        viewModel.resetAddCardState()
                    }
                    else -> {}
                }
            }
        }
        
        // Observe validate state
        lifecycleScope.launch {
            viewModel.validateState.collect { state ->
                when (state) {
                    is ValidateState.Loading -> {
                        showLoader()
                    }
                    is ValidateState.Success -> {
                        hideLoader()
                        validateResponse = state.response
                        validatedTransactionId = state.response.data?.transactionId
                        Toast.makeText(
                            this@MakePaymentActivity,
                            "Validation successful. Transaction ID: ${validatedTransactionId ?: "N/A"}",
                            Toast.LENGTH_LONG
                        ).show()
                        viewModel.resetValidateState()
                    }
                    is ValidateState.Error -> {
                        hideLoader()
                        Toast.makeText(
                            this@MakePaymentActivity,
                            "Validation failed: ${state.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        viewModel.resetValidateState()
                    }
                    else -> {}
                }
            }
        }
        
        // Observe finalize state
        lifecycleScope.launch {
            viewModel.finalizeState.collect { state ->
                when (state) {
                    is FinalizeState.Loading -> {
                        showLoader()
                    }
                    is FinalizeState.Success -> {
                        hideLoader()
                        val confirmationNumber = state.response.data?.confirmationNumber
                        Toast.makeText(
                            this@MakePaymentActivity,
                            "Payment finalised successfully. Confirmation: ${confirmationNumber ?: "N/A"}",
                            Toast.LENGTH_LONG
                        ).show()
                        // Clear after successful finalisation
                        validatedTransactionId = null
                        validateResponse = null
                        viewModel.resetFinalizeState()
                    }
                    is FinalizeState.Error -> {
                        hideLoader()
                        Toast.makeText(
                            this@MakePaymentActivity,
                            "Finalisation failed: ${state.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        viewModel.resetFinalizeState()
                    }
                    else -> {}
                }
            }
        }
    }
    
    private fun showLoader() {
        binding.psdkLoaderOverlay.visibility = View.VISIBLE
    }
    
    private fun hideLoader() {
        binding.psdkLoaderOverlay.visibility = View.GONE
    }
    
    private fun showAddCardDialog() {
        val dialog = Dialog(this, R.style.PaymentDialogTheme)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        
        val dialogBinding = PsdkDialogAddCardBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)
        
        dialog.window?.apply {
            setBackgroundDrawableResource(R.drawable.bg_dialog)
            setLayout(
                (resources.displayMetrics.widthPixels * 0.9).toInt(),
                android.view.WindowManager.LayoutParams.WRAP_CONTENT
            )
        }
        
        dialogBinding.psdkBtnCancel.setOnClickListener {
            dialog.dismiss()
        }
        
        dialogBinding.psdkBtnAddCard.setOnClickListener {
            val cardNumber = dialogBinding.psdkEtCardNumber.text.toString().replace(" ", "")
            val expiry = dialogBinding.psdkEtExpiry.text.toString()
            val cvv = dialogBinding.psdkEtCvv.text.toString()
            val cardHolder = dialogBinding.psdkEtCardHolder.text.toString()
            
            if (validateCard(dialogBinding, cardNumber, expiry, cvv, cardHolder)) {
                val (month, year) = parseExpiry(expiry)
                viewModel.addPaymentMethod(cardNumber, month, year, cvv, cardHolder)
                dialog.dismiss()
            }
        }
        
        dialog.show()
    }
    
    private fun validateCard(
        binding: PsdkDialogAddCardBinding,
        cardNumber: String,
        expiry: String,
        cvv: String,
        cardHolder: String
    ): Boolean {
        var valid = true
        
        if (cardNumber.length < 13) {
            binding.psdkCardNumberLayout.error = getString(R.string.error_invalid_card)
            valid = false
        } else {
            binding.psdkCardNumberLayout.error = null
        }
        
        if (!expiry.matches(Regex("\\d{2}/\\d{2}"))) {
            binding.psdkExpiryLayout.error = getString(R.string.error_invalid_expiry)
            valid = false
        } else {
            binding.psdkExpiryLayout.error = null
        }
        
        if (cvv.length < 3) {
            binding.psdkCvvLayout.error = getString(R.string.error_invalid_cvv)
            valid = false
        } else {
            binding.psdkCvvLayout.error = null
        }
        
        if (cardHolder.isBlank()) {
            binding.psdkCardHolderLayout.error = getString(R.string.error_name_required)
            valid = false
        } else {
            binding.psdkCardHolderLayout.error = null
        }
        
        return valid
    }
    
    private fun parseExpiry(expiry: String): Pair<Int, Int> {
        val parts = expiry.split("/")
        val month = parts.getOrNull(0)?.toIntOrNull() ?: 1
        val year = (parts.getOrNull(1)?.toIntOrNull() ?: 0) + 2000
        return month to year
    }
    
    private fun callValidateApi() {
        val request = paymentRequest ?: run {
            Toast.makeText(this, "Payment request not available", Toast.LENGTH_LONG).show()
            return
        }
        
        // TODO: Replace these placeholder values with actual data from your payment flow
        // These values should come from your payment configuration or user selection
        val validationUrl = "https://apple-pay-gateway-cert.apple.com/paymentservices/startSession"
        val cid = 4547L // TODO: Get from PaymentSDK config or payment request
        val propertyId = 57711L // TODO: Get from PaymentSDK config or payment request
        val customerId = PaymentSDK.getCustomerId()
        val leaseId = 15456361L // TODO: Get from PaymentSDK config or payment request
        val amount = request.amount
        
        viewModel.validatePayment(
            validationUrl = validationUrl,
            cid = cid,
            propertyId = propertyId,
            customerId = customerId,
            leaseId = leaseId,
            amount = amount
        )
    }
    
    private fun callFinaliseApi() {
        val request = paymentRequest ?: run {
            Toast.makeText(this, "Payment request not available", Toast.LENGTH_LONG).show()
            return
        }
        
        val validateResp = validateResponse
        if (validateResp == null || validatedTransactionId.isNullOrBlank()) {
            Toast.makeText(
                this,
                "No validated transaction found. Please validate payment first.",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        
        // TODO: Replace these placeholder values with actual data from your payment flow
        // These values should come from your payment configuration, Apple Pay response, or user input
        val cid = 4547L // TODO: Get from PaymentSDK config or payment request
        val propertyId = 57711L // TODO: Get from PaymentSDK config or payment request
        val customerId = PaymentSDK.getCustomerId()
        val leaseId = 15456361L // TODO: Get from PaymentSDK config or payment request
        val amount = request.amount
        
        // TODO: Replace with actual payment token from Apple Pay or payment method
        // This is a complex object that should come from the payment provider response
        val paymentToken = FinalizeRequest.PaymentToken(
            paymentData = FinalizeRequest.PaymentToken.PaymentData(
                data = "PLACEHOLDER_PAYMENT_DATA",
                signature = "PLACEHOLDER_SIGNATURE",
                header = FinalizeRequest.PaymentToken.PaymentData.PaymentHeader(
                    publicKeyHash = "PLACEHOLDER_PUBLIC_KEY_HASH",
                    ephemeralPublicKey = "PLACEHOLDER_EPHEMERAL_PUBLIC_KEY",
                    transactionId = validatedTransactionId ?: ""
                ),
                version = "EC_v1"
            ),
            paymentMethod = FinalizeRequest.PaymentToken.PaymentMethodTokenInfo(
                displayName = "MasterCard 7599",
                network = "MasterCard",
                type = "credit"
            ),
            transactionIdentifier = validatedTransactionId ?: ""
        )
        
        // TODO: Replace with actual customer billing information
        val billToCustomer = FinalizeRequest.BillToCustomer(
            firstName = "John",
            lastName = "Doe",
            postalCode = "12345",
            countryCode = "US",
            email = "",
            city = "City",
            state = "ST",
            addressLine1 = "123 Main St"
        )
        
        val paymentAmounts = FinalizeRequest.PaymentAmounts(
            amount = String.format("%.2f", amount),
            convenienceFee = "0.06"
        )
        
        val finalizeRequest = FinalizeRequest(
            data = FinalizeRequest.FinalizeRequestData(
                cid = cid,
                propertyId = propertyId,
                customerId = customerId,
                leaseId = leaseId,
                amount = amount,
                paymentToken = paymentToken,
                billToCustomer = billToCustomer,
                totalAmount = String.format("%.2f", amount + 0.06),
                paymentAmounts = paymentAmounts,
                agreesToFees = true,
                agreesToTerms = true,
                termsAcceptedOn = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                }.format(java.util.Date()),
                provider = "apple_pay"
            )
        )
        
        viewModel.finalizePayment(finalizeRequest)
    }
    
    private fun processPayment() {
        val request = paymentRequest ?: return
        viewModel.processPayment(request.amount, request.currency, request.description)
    }
    
    private fun showSuccessAndFinish(transaction: com.payment.sdk.models.PaymentTransaction) {
        // Show success UI
        binding.psdkContentLayout.visibility = View.GONE
        binding.psdkSuccessLayout.visibility = View.VISIBLE
        
        binding.psdkTvSuccessAmount.text = NumberFormat.getCurrencyInstance(Locale.US).format(transaction.amount)
        binding.psdkTvTransactionId.text = transaction.transactionId
        binding.psdkTvConfirmationNumber.text = transaction.confirmationNumber ?: "-"
        
        binding.psdkBtnDone.setOnClickListener {
            finishWithResult(
                PaymentResult.Success(
                    transactionId = transaction.transactionId,
                    confirmationNumber = transaction.confirmationNumber,
                    amount = transaction.amount,
                    currency = transaction.currency
                )
            )
        }
    }
    
    private fun finishWithResult(result: PaymentResult) {
        if (isFinishing) {
            return
        }
        
        PaymentSDK.notifyResult(result)
        
        val data = Intent().apply {
            putExtra(PaymentSDK.EXTRA_PAYMENT_RESULT, result)
        }
        setResult(RESULT_OK, data)
        
        // Post finish to ensure activity is fully created and transition has started
        // This prevents "non-playing transition" errors when finishing too early
        binding.root.post {
            if (!isFinishing) {
                try {
                    finish()
                } catch (e: IllegalStateException) {
                    // Activity may already be finishing, ignore
                    android.util.Log.w("MakePaymentActivity", "Failed to finish activity", e)
                }
            }
        }
    }
    
    private fun finishWithError(code: String, message: String) {
        finishWithResult(PaymentResult.Failure(code, message))
    }
    
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Handle back press ourselves to set result before finishing
        // Don't call super to avoid double-finish conflicts
        finishWithResult(PaymentResult.Cancelled)
    }
}
