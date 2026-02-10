package com.amenitypay.sdk.ui.payment

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.amenitypay.sdk.R
import com.amenitypay.sdk.databinding.ItemPaymentMethodBinding
import com.amenitypay.sdk.models.CardType
import com.amenitypay.sdk.models.CustomerPaymentAccount

/**
 * Adapter for displaying payment accounts in a RecyclerView
 */
class PaymentMethodsAdapter(
    private val onItemClick: (CustomerPaymentAccount) -> Unit
) : ListAdapter<CustomerPaymentAccount, PaymentMethodsAdapter.ViewHolder>(PaymentAccountDiffCallback()) {
    
    private var selectedAccountId: Long? = null
    
    fun setSelectedPaymentAccount(accountId: Long) {
        val previousSelected = selectedAccountId
        selectedAccountId = accountId
        
        // Notify changes for previous and new selection
        currentList.forEachIndexed { index, account ->
            if (account.customerPaymentAccountId == previousSelected || 
                account.customerPaymentAccountId == accountId) {
                notifyItemChanged(index)
            }
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPaymentMethodBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val account = getItem(position)
        val isSelected = account.customerPaymentAccountId == selectedAccountId
        holder.bind(account, isSelected, onItemClick)
    }
    
    class ViewHolder(
        private val binding: ItemPaymentMethodBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(
            account: CustomerPaymentAccount,
            isSelected: Boolean,
            onItemClick: (CustomerPaymentAccount) -> Unit
        ) {
            // Card icon based on type
            val iconRes = when (account.cardType) {
                CardType.VISA -> R.drawable.ic_visa
                CardType.MASTERCARD -> R.drawable.ic_mastercard
                CardType.AMEX -> R.drawable.ic_amex
                else -> R.drawable.ic_card_generic
            }
            binding.ivCardIcon.setImageResource(iconRes)
            
            // Card details
            binding.tvCardNumber.text = account.maskedNumber
            binding.tvCardHolder.text = account.cardHolderName
            binding.tvExpiry.text = binding.root.context.getString(
                R.string.expires_label,
                account.expiryDate
            )
            
            // Default badge
            binding.tvDefaultBadge.visibility = if (account.isDefault) View.VISIBLE else View.GONE
            
            // Selection state
            binding.selectionIndicator.visibility = if (isSelected) View.VISIBLE else View.INVISIBLE
            binding.cardContainer.setBackgroundResource(
                if (isSelected) R.drawable.bg_card_selected else R.drawable.bg_card_unselected
            )
            
            // Click listener
            binding.root.setOnClickListener {
                onItemClick(account)
            }
        }
    }
    
    private class PaymentAccountDiffCallback : DiffUtil.ItemCallback<CustomerPaymentAccount>() {
        override fun areItemsTheSame(oldItem: CustomerPaymentAccount, newItem: CustomerPaymentAccount): Boolean {
            return oldItem.customerPaymentAccountId == newItem.customerPaymentAccountId
        }
        
        override fun areContentsTheSame(oldItem: CustomerPaymentAccount, newItem: CustomerPaymentAccount): Boolean {
            return oldItem == newItem
        }
    }
}
