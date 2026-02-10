package com.paymentmethod.sdk.ui

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
import com.paymentmethod.sdk.PaymentMethodSDK
import com.paymentmethod.sdk.R
import com.paymentmethod.sdk.databinding.PmsdkActivitySelectorBinding
import com.paymentmethod.sdk.databinding.PmsdkDialogAddCardBinding
import com.paymentmethod.sdk.models.PaymentMethod
import kotlinx.coroutines.launch

/**
 * Activity for selecting or adding payment methods.
 */
class PaymentMethodSelectorActivity : AppCompatActivity() {
    
    private lateinit var binding: PmsdkActivitySelectorBinding
    private lateinit var viewModel: PaymentMethodSelectorViewModel
    private lateinit var adapter: PaymentMethodAdapter
    
    private var selectionMode: SelectionMode = SelectionMode.SELECT
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply custom theme if set
        PaymentMethodSDK.getCustomTheme()?.let { themeRes ->
            setTheme(themeRes)
        }
        
        super.onCreate(savedInstanceState)
        binding = PmsdkActivitySelectorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Apply style config colors if available
        applyStyleConfig()
        
        // Get selection mode from intent
        selectionMode = SelectionMode.valueOf(
            intent.getStringExtra(EXTRA_SELECTION_MODE) ?: SelectionMode.SELECT.name
        )
        
        setupViewModel()
        setupViews()
        observeState()
    }
    
    private fun applyStyleConfig() {
        val styleConfig = PaymentMethodSDK.getStyleConfig()
        
        // Apply primary color to toolbar if set
        styleConfig.primaryColor?.let { color ->
            binding.psdkToolbar.setBackgroundColor(color)
        }
        
        // Apply background color if set
        styleConfig.backgroundColor?.let { color ->
            binding.root.setBackgroundColor(color)
        }
    }
    
    private fun setupViewModel() {
        viewModel = ViewModelProvider(
            this,
            PaymentMethodSelectorViewModelFactory(PaymentMethodSDK.getRepository())
        )[PaymentMethodSelectorViewModel::class.java]
    }
    
    private fun setupViews() {
        // Title based on mode
        binding.psdkTvTitle.text = when (selectionMode) {
            SelectionMode.SELECT -> getString(R.string.pmsdk_select_payment_method)
            SelectionMode.MANAGE -> getString(R.string.pmsdk_manage_payment_methods)
            SelectionMode.ADD_ONLY -> getString(R.string.pmsdk_add_payment_method)
        }
        
        // Navigation
        binding.psdkBtnBack.setOnClickListener {
            finishWithCancelled()
        }
        
        binding.psdkBtnClose.setOnClickListener {
            finishWithCancelled()
        }
        
        // Payment methods list
        adapter = PaymentMethodAdapter { method ->
            viewModel.selectPaymentMethod(method.id)
        }
        
        binding.psdkRvPaymentMethods.apply {
            layoutManager = LinearLayoutManager(this@PaymentMethodSelectorActivity)
            adapter = this@PaymentMethodSelectorActivity.adapter
        }
        
        // Add card button
        binding.psdkBtnAddCard.setOnClickListener {
            showAddCardDialog()
        }
        
        // Confirm button
        binding.psdkBtnConfirm.setOnClickListener {
            confirmSelection()
        }
        
        // Retry button
        binding.psdkBtnRetry.setOnClickListener {
            viewModel.loadPaymentMethods()
        }
        
        // Show/hide confirm button based on mode
        binding.psdkBtnConfirm.visibility = when (selectionMode) {
            SelectionMode.SELECT -> View.VISIBLE
            SelectionMode.MANAGE -> View.GONE
            SelectionMode.ADD_ONLY -> View.GONE
        }
        
        // If add-only mode, show dialog immediately
        if (selectionMode == SelectionMode.ADD_ONLY) {
            showAddCardDialog()
        }
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
                binding.psdkBtnConfirm.isEnabled = methodId != null
            }
        }
        
        // Observe add card state
        lifecycleScope.launch {
            viewModel.addCardState.collect { state ->
                when (state) {
                    is AddCardState.Added -> {
                        Toast.makeText(
                            this@PaymentMethodSelectorActivity,
                            R.string.pmsdk_card_added,
                            Toast.LENGTH_SHORT
                        ).show()
                        
                        // If add-only mode, finish with the new card
                        if (selectionMode == SelectionMode.ADD_ONLY) {
                            finishWithSelected(state.paymentMethod)
                        }
                        
                        viewModel.resetAddCardState()
                    }
                    is AddCardState.Error -> {
                        Toast.makeText(
                            this@PaymentMethodSelectorActivity,
                            state.message,
                            Toast.LENGTH_LONG
                        ).show()
                        viewModel.resetAddCardState()
                    }
                    else -> {}
                }
            }
        }
    }
    
    private fun showAddCardDialog() {
        val dialog = Dialog(this, R.style.PaymentMethodDialogTheme)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        
        val dialogBinding = PmsdkDialogAddCardBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)
        
        dialog.window?.apply {
            setBackgroundDrawableResource(R.drawable.pmsdk_bg_dialog)
            setLayout(
                (resources.displayMetrics.widthPixels * 0.9).toInt(),
                android.view.WindowManager.LayoutParams.WRAP_CONTENT
            )
        }
        
        dialogBinding.pmsdkBtnCancel.setOnClickListener {
            dialog.dismiss()
            if (selectionMode == SelectionMode.ADD_ONLY) {
                finishWithCancelled()
            }
        }
        
        dialogBinding.pmsdkBtnAddCard.setOnClickListener {
            val cardNumber = dialogBinding.pmsdkEtCardNumber.text.toString().replace(" ", "")
            val expiry = dialogBinding.pmsdkEtExpiry.text.toString()
            val cvv = dialogBinding.pmsdkEtCvv.text.toString()
            val cardHolder = dialogBinding.pmsdkEtCardHolder.text.toString()
            
            if (validateCard(dialogBinding, cardNumber, expiry, cvv, cardHolder)) {
                val (month, year) = parseExpiry(expiry)
                viewModel.addPaymentMethod(cardNumber, month, year, cvv, cardHolder)
                dialog.dismiss()
            }
        }
        
        dialog.setOnCancelListener {
            if (selectionMode == SelectionMode.ADD_ONLY) {
                finishWithCancelled()
            }
        }
        
        dialog.show()
    }
    
    private fun validateCard(
        binding: PmsdkDialogAddCardBinding,
        cardNumber: String,
        expiry: String,
        cvv: String,
        cardHolder: String
    ): Boolean {
        var valid = true
        
        if (cardNumber.length < 13) {
            binding.pmsdkCardNumberLayout.error = getString(R.string.pmsdk_error_invalid_card)
            valid = false
        } else {
            binding.pmsdkCardNumberLayout.error = null
        }
        
        if (!expiry.matches(Regex("\\d{2}/\\d{2}"))) {
            binding.pmsdkExpiryLayout.error = getString(R.string.pmsdk_error_invalid_expiry)
            valid = false
        } else {
            binding.pmsdkExpiryLayout.error = null
        }
        
        if (cvv.length < 3) {
            binding.pmsdkCvvLayout.error = getString(R.string.pmsdk_error_invalid_cvv)
            valid = false
        } else {
            binding.pmsdkCvvLayout.error = null
        }
        
        if (cardHolder.isBlank()) {
            binding.pmsdkCardHolderLayout.error = getString(R.string.pmsdk_error_name_required)
            valid = false
        } else {
            binding.pmsdkCardHolderLayout.error = null
        }
        
        return valid
    }
    
    private fun parseExpiry(expiry: String): Pair<Int, Int> {
        val parts = expiry.split("/")
        val month = parts.getOrNull(0)?.toIntOrNull() ?: 1
        val year = (parts.getOrNull(1)?.toIntOrNull() ?: 0) + 2000
        return month to year
    }
    
    private fun confirmSelection() {
        val selectedMethod = viewModel.getSelectedPaymentMethod()
        if (selectedMethod != null) {
            finishWithSelected(selectedMethod)
        } else {
            Toast.makeText(this, R.string.pmsdk_error_select_payment, Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun finishWithSelected(paymentMethod: PaymentMethod) {
        PaymentMethodSDK.notifyMethodSelected(paymentMethod)
        
        val data = Intent().apply {
            putExtra(EXTRA_SELECTED_PAYMENT_METHOD, paymentMethod)
        }
        setResult(RESULT_OK, data)
        finish()
    }
    
    private fun finishWithCancelled() {
        PaymentMethodSDK.notifyCancelled()
        setResult(RESULT_CANCELED)
        finish()
    }
    
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        finishWithCancelled()
    }
    
    /**
     * Selection mode for the activity.
     */
    enum class SelectionMode {
        SELECT,     // Select an existing payment method
        MANAGE,     // Manage payment methods (no confirm button)
        ADD_ONLY    // Add a new payment method only
    }
    
    companion object {
        const val EXTRA_SELECTION_MODE = "extra_selection_mode"
        const val EXTRA_SELECTED_PAYMENT_METHOD = "extra_selected_payment_method"
    }
}
