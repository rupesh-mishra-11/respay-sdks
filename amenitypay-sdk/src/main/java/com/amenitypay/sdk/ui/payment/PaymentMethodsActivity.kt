package com.amenitypay.sdk.ui.payment

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
import com.amenitypay.sdk.AmenityPaySDK
import com.amenitypay.sdk.R
import com.amenitypay.sdk.databinding.ActivityPaymentMethodsBinding
import com.amenitypay.sdk.databinding.DialogAddCardBinding
import com.amenitypay.sdk.models.AddPaymentAccountRequest
import com.amenitypay.sdk.models.AmenityBookingRequest
import com.amenitypay.sdk.models.AmenityPayResult
import com.amenitypay.sdk.models.CustomerPaymentAccount
import com.amenitypay.sdk.ui.checkout.CheckoutActivity
import com.amenitypay.sdk.SDKConstants
import kotlinx.coroutines.launch

/**
 * Activity to display and select payment accounts
 */
class PaymentMethodsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityPaymentMethodsBinding
    private lateinit var viewModel: PaymentMethodsViewModel
    private lateinit var adapter: PaymentMethodsAdapter
    
    private var bookingRequest: AmenityBookingRequest? = null
    private var selectedPaymentAccount: CustomerPaymentAccount? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentMethodsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        @Suppress("DEPRECATION")
        bookingRequest = intent.getSerializableExtra(EXTRA_BOOKING_REQUEST) as? AmenityBookingRequest
        
        setupViewModel()
        setupViews()
        observeState()
        
        viewModel.loadPaymentAccounts()
        viewModel.loadCustomerProfile()
    }
    
    private fun setupViewModel() {
        viewModel = ViewModelProvider(
            this,
            PaymentMethodsViewModelFactory(
                AmenityPaySDK.getAuthRepository(),
                AmenityPaySDK.getPaymentRepository()
            )
        )[PaymentMethodsViewModel::class.java]
    }
    
    private fun setupViews() {
        // Navigation
        binding.btnBack.setOnClickListener {
            setResultAndFinish(AmenityPayResult.Cancelled)
        }
        
        binding.btnClose.setOnClickListener {
            setResultAndFinish(AmenityPayResult.Cancelled)
        }
        
        // RecyclerView
        adapter = PaymentMethodsAdapter { paymentAccount ->
            selectedPaymentAccount = paymentAccount
            adapter.setSelectedPaymentAccount(paymentAccount.customerPaymentAccountId)
        }
        
        binding.rvPaymentMethods.apply {
            layoutManager = LinearLayoutManager(this@PaymentMethodsActivity)
            adapter = this@PaymentMethodsActivity.adapter
        }
        
        // Add Card Button
        binding.btnAddCard.setOnClickListener {
            showAddCardDialog()
        }
        
        // Continue Button
        binding.btnContinue.setOnClickListener {
            if (selectedPaymentAccount != null) {
                navigateToCheckout()
            } else {
                Toast.makeText(this, R.string.error_select_payment, Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun observeState() {
        lifecycleScope.launch {
            viewModel.paymentAccountsState.collect { state ->
                when (state) {
                    is PaymentAccountsState.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.rvPaymentMethods.visibility = View.GONE
                        binding.emptyState.visibility = View.GONE
                    }
                    is PaymentAccountsState.Success -> {
                        binding.progressBar.visibility = View.GONE
                        
                        if (state.paymentAccounts.isEmpty()) {
                            binding.rvPaymentMethods.visibility = View.GONE
                            binding.emptyState.visibility = View.VISIBLE
                        } else {
                            binding.rvPaymentMethods.visibility = View.VISIBLE
                            binding.emptyState.visibility = View.GONE
                            adapter.submitList(state.paymentAccounts)
                            
                            // Auto-select default payment account
                            if (selectedPaymentAccount == null) {
                                val defaultAccount = state.paymentAccounts.find { it.isDefault }
                                    ?: state.paymentAccounts.firstOrNull()
                                defaultAccount?.let {
                                    selectedPaymentAccount = it
                                    adapter.setSelectedPaymentAccount(it.customerPaymentAccountId)
                                }
                            }
                        }
                    }
                    is PaymentAccountsState.Error -> {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(this@PaymentMethodsActivity, state.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
        
        lifecycleScope.launch {
            viewModel.customerProfile.collect { profile ->
                profile?.let {
                    binding.tvUserName.text = it.fullName
                    binding.tvUserEmail.text = it.email ?: "Customer #${it.customerId}"
                }
            }
        }
    }
    
    private fun showAddCardDialog() {
        val dialog = Dialog(this, R.style.AmenityDialogTheme)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(true)
        
        val dialogBinding = DialogAddCardBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)
        
        // Set dialog window properties
        dialog.window?.apply {
            setBackgroundDrawableResource(R.drawable.bg_dialog)
            setLayout(
                (resources.displayMetrics.widthPixels * 0.9).toInt(),
                android.view.WindowManager.LayoutParams.WRAP_CONTENT
            )
        }
        
        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        
        dialogBinding.btnAddCard.setOnClickListener {
            val cardNumber = dialogBinding.etCardNumber.text.toString().replace(" ", "")
            val expiry = dialogBinding.etExpiry.text.toString()
            val cvv = dialogBinding.etCvv.text.toString()
            val cardHolder = dialogBinding.etCardHolder.text.toString()
            
            if (validateCardInput(dialogBinding, cardNumber, expiry, cvv, cardHolder)) {
                val (month, year) = parseExpiry(expiry)
                val credentials = AmenityPaySDK.getAuthCredentials()
                
                if (credentials != null) {
                    val request = AddPaymentAccountRequest(
                        customerId = credentials.customerId,
                        cardNumber = cardNumber,
                        expiryMonth = month,
                        expiryYear = year,
                        cvv = cvv,
                        cardHolderName = cardHolder
                    )
                    viewModel.addPaymentAccount(request)
                    dialog.dismiss()
                }
            }
        }
        
        dialog.show()
    }
    
    private fun validateCardInput(
        dialogBinding: DialogAddCardBinding,
        cardNumber: String,
        expiry: String,
        cvv: String,
        cardHolder: String
    ): Boolean {
        var isValid = true
        
        if (cardNumber.length < 13 || cardNumber.length > 19) {
            dialogBinding.cardNumberLayout.error = "Invalid card number"
            isValid = false
        }
        
        if (!expiry.matches(Regex("\\d{2}/\\d{2}"))) {
            dialogBinding.expiryLayout.error = "Use MM/YY format"
            isValid = false
        }
        
        if (cvv.length < 3 || cvv.length > 4) {
            dialogBinding.cvvLayout.error = "Invalid CVV"
            isValid = false
        }
        
        if (cardHolder.isBlank()) {
            dialogBinding.cardHolderLayout.error = "Name is required"
            isValid = false
        }
        
        return isValid
    }
    
    private fun parseExpiry(expiry: String): Pair<Int, Int> {
        val parts = expiry.split("/")
        val month = parts.getOrNull(0)?.toIntOrNull() ?: 1
        val year = (parts.getOrNull(1)?.toIntOrNull() ?: 0) + 2000
        return month to year
    }
    
    private fun navigateToCheckout() {
        val intent = Intent(this, CheckoutActivity::class.java).apply {
            putExtra(CheckoutActivity.EXTRA_BOOKING_REQUEST, bookingRequest)
            putExtra(CheckoutActivity.EXTRA_PAYMENT_ACCOUNT_ID, selectedPaymentAccount?.customerPaymentAccountId)
            putExtra(CheckoutActivity.EXTRA_PAYMENT_TYPE_ID, selectedPaymentAccount?.paymentTypeId)
            putExtra(CheckoutActivity.EXTRA_PAYMENT_ACCOUNT_LAST_FOUR, selectedPaymentAccount?.lastFour)
            putExtra(CheckoutActivity.EXTRA_PAYMENT_ACCOUNT_TYPE, selectedPaymentAccount?.cardType?.name)
            putExtra(CheckoutActivity.EXTRA_PAYMENT_ACCOUNT_EXPIRY, selectedPaymentAccount?.expiryDate)
        }
        startActivityForResult(intent, REQUEST_CHECKOUT)
    }
    
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CHECKOUT) {
            @Suppress("DEPRECATION")
            val result = data?.getSerializableExtra(SDKConstants.EXTRA_RESULT) as? AmenityPayResult
            if (result != null && result !is AmenityPayResult.Cancelled) {
                setResultAndFinish(result)
            }
        }
    }
    
    private fun setResultAndFinish(result: AmenityPayResult) {
        val data = Intent().apply {
            putExtra(SDKConstants.EXTRA_RESULT, result)
        }
        setResult(RESULT_OK, data)
        finish()
    }
    
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        setResultAndFinish(AmenityPayResult.Cancelled)
    }
    
    companion object {
        const val EXTRA_BOOKING_REQUEST = "extra_booking_request"
        const val REQUEST_CHECKOUT = 1002
    }
}
