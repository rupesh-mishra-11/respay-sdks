package com.amenitypay.sdk.ui.checkout

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.amenitypay.sdk.AmenityPaySDK
import com.amenitypay.sdk.R
import com.amenitypay.sdk.databinding.ActivityCheckoutBinding
import com.amenitypay.sdk.models.AmenityBookingRequest
import com.amenitypay.sdk.models.AmenityPayResult
import com.amenitypay.sdk.models.CardType
import com.amenitypay.sdk.models.CustomerPaymentAccount
import com.amenitypay.sdk.SDKConstants
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Checkout activity for processing amenity payments
 */
class CheckoutActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityCheckoutBinding
    private lateinit var viewModel: CheckoutViewModel
    
    private var bookingRequest: AmenityBookingRequest? = null
    private var paymentAccountId: Long? = null
    private var paymentTypeId: Long? = null
    private var paymentAccountLastFour: String? = null
    private var paymentAccountType: String? = null
    private var paymentAccountExpiry: String? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCheckoutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        extractIntentData()
        setupViewModel()
        setupViews()
        observeState()
        displayBookingDetails()
    }
    
    private fun extractIntentData() {
        @Suppress("DEPRECATION")
        bookingRequest = intent.getSerializableExtra(EXTRA_BOOKING_REQUEST) as? AmenityBookingRequest
        paymentAccountId = intent.getLongExtra(EXTRA_PAYMENT_ACCOUNT_ID, -1).takeIf { it != -1L }
        paymentTypeId = intent.getLongExtra(EXTRA_PAYMENT_TYPE_ID, -1).takeIf { it != -1L }
        paymentAccountLastFour = intent.getStringExtra(EXTRA_PAYMENT_ACCOUNT_LAST_FOUR)
        paymentAccountType = intent.getStringExtra(EXTRA_PAYMENT_ACCOUNT_TYPE)
        paymentAccountExpiry = intent.getStringExtra(EXTRA_PAYMENT_ACCOUNT_EXPIRY)
    }
    
    private fun setupViewModel() {
        viewModel = ViewModelProvider(
            this,
            CheckoutViewModelFactory(AmenityPaySDK.getPaymentRepository())
        )[CheckoutViewModel::class.java]
    }
    
    private fun setupViews() {
        // Navigation
        binding.btnBack.setOnClickListener {
            finish()
        }
        
        binding.btnClose.setOnClickListener {
            setResultAndFinish(AmenityPayResult.Cancelled)
        }
        
        // Change payment method
        binding.btnChangePayment.setOnClickListener {
            finish() // Go back to payment methods screen
        }
        
        // Pay Now button
        binding.btnPayNow.setOnClickListener {
            processPayment()
        }
        
        // Done button (on success screen)
        binding.btnDone.setOnClickListener {
            val result = viewModel.getSuccessResult()
            if (result != null) {
                setResultAndFinish(result)
            }
        }
    }
    
    private fun displayBookingDetails() {
        bookingRequest?.let { request ->
            // Amenity name
            binding.tvAmenityName.text = request.amenityName
            
            // Date/Time formatting
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val outputFormat = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.US)
            
            try {
                val fromDate = inputFormat.parse(request.startDatetime)
                val toDate = inputFormat.parse(request.endDatetime)
                binding.tvFromDateTime.text = fromDate?.let { outputFormat.format(it) } ?: request.startDatetime
                binding.tvToDateTime.text = toDate?.let { outputFormat.format(it) } ?: request.endDatetime
            } catch (e: Exception) {
                binding.tvFromDateTime.text = request.startDatetime
                binding.tvToDateTime.text = request.endDatetime
            }
            
            // Amount
            val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
            binding.tvAmount.text = currencyFormat.format(request.amount)
            
            // Update pay button text
            binding.btnPayNow.text = getString(R.string.btn_pay_now) + " " + currencyFormat.format(request.amount)
        }
        
        // Payment method display
        displayPaymentMethod()
    }
    
    private fun displayPaymentMethod() {
        // Card icon
        val cardType = try {
            CardType.valueOf(paymentAccountType ?: "UNKNOWN")
        } catch (e: Exception) {
            CardType.UNKNOWN
        }
        
        val iconRes = when (cardType) {
            CardType.VISA -> R.drawable.ic_visa
            CardType.MASTERCARD -> R.drawable.ic_mastercard
            CardType.AMEX -> R.drawable.ic_amex
            else -> R.drawable.ic_card_generic
        }
        binding.ivCardIcon.setImageResource(iconRes)
        
        // Card number
        binding.tvSelectedCardNumber.text = "•••• •••• •••• ${paymentAccountLastFour ?: "****"}"
        
        // Expiry
        binding.tvSelectedCardExpiry.text = getString(R.string.expires_label, paymentAccountExpiry ?: "--/--")
    }
    
    private fun processPayment() {
        val request = bookingRequest ?: return
        val accountId = paymentAccountId ?: return
        val typeId = paymentTypeId ?: return
        
        // Create a minimal payment account for the request
        val paymentAccount = CustomerPaymentAccount(
            customerPaymentAccountId = accountId,
            paymentTypeId = typeId,
            paymentTypeName = null,
            cardType = try { CardType.valueOf(paymentAccountType ?: "UNKNOWN") } catch (e: Exception) { CardType.UNKNOWN },
            lastFour = paymentAccountLastFour ?: "",
            expiryMonth = 0,
            expiryYear = 0,
            cardHolderName = "",
            isDefault = false,
            isActive = true
        )
        
        viewModel.processPayment(request, paymentAccount)
    }
    
    private fun observeState() {
        lifecycleScope.launch {
            viewModel.checkoutState.collect { state ->
                when (state) {
                    is CheckoutState.Idle -> {
                        binding.loadingOverlay.visibility = View.GONE
                        binding.successOverlay.visibility = View.GONE
                        binding.btnPayNow.isEnabled = true
                    }
                    is CheckoutState.Processing -> {
                        binding.loadingOverlay.visibility = View.VISIBLE
                        binding.successOverlay.visibility = View.GONE
                        binding.btnPayNow.isEnabled = false
                    }
                    is CheckoutState.Success -> {
                        binding.loadingOverlay.visibility = View.GONE
                        binding.successOverlay.visibility = View.VISIBLE
                        binding.tvConfirmationNumber.text = getString(
                            R.string.confirmation_number,
                            state.confirmationNumber ?: state.transactionId
                        )
                    }
                    is CheckoutState.Error -> {
                        binding.loadingOverlay.visibility = View.GONE
                        binding.successOverlay.visibility = View.GONE
                        binding.btnPayNow.isEnabled = true
                        Toast.makeText(this@CheckoutActivity, state.message, Toast.LENGTH_LONG).show()
                    }
                }
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
        // If success is showing, return the result
        val successResult = viewModel.getSuccessResult()
        if (successResult != null) {
            setResultAndFinish(successResult)
            return
        }
        
        // If processing, don't allow back
        if (viewModel.checkoutState.value is CheckoutState.Processing) {
            return
        }
        
        super.onBackPressed()
    }
    
    companion object {
        const val EXTRA_BOOKING_REQUEST = "extra_booking_request"
        const val EXTRA_PAYMENT_ACCOUNT_ID = "extra_payment_account_id"
        const val EXTRA_PAYMENT_TYPE_ID = "extra_payment_type_id"
        const val EXTRA_PAYMENT_ACCOUNT_LAST_FOUR = "extra_payment_account_last_four"
        const val EXTRA_PAYMENT_ACCOUNT_TYPE = "extra_payment_account_type"
        const val EXTRA_PAYMENT_ACCOUNT_EXPIRY = "extra_payment_account_expiry"
    }
}
