package com.paymentmethod.sdk.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.paymentmethod.sdk.R
import com.paymentmethod.sdk.models.CardType
import com.paymentmethod.sdk.models.PaymentMethod

/**
 * RecyclerView adapter for displaying payment methods.
 * Uses manual view binding to avoid resource ID conflicts in SDK.
 */
class PaymentMethodAdapter(
    private val onMethodSelected: (PaymentMethod) -> Unit
) : ListAdapter<PaymentMethod, PaymentMethodAdapter.ViewHolder>(DiffCallback()) {
    
    private var selectedId: Long? = null
    
    /**
     * Set the currently selected payment method ID.
     */
    fun setSelectedId(id: Long?) {
        val oldId = selectedId
        selectedId = id
        
        currentList.forEachIndexed { index, method ->
            if (method.id == oldId || method.id == id) {
                notifyItemChanged(index)
            }
        }
    }
    
    /**
     * Get the currently selected payment method ID.
     */
    fun getSelectedId(): Long? = selectedId
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.pmsdk_item_payment_method, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), selectedId)
    }
    
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        
        private val cardView: MaterialCardView = itemView as MaterialCardView
        private val ivCardIcon: ImageView = itemView.findViewById(R.id.pmsdk_ivCardIcon)
        private val tvCardNumber: TextView = itemView.findViewById(R.id.pmsdk_tvCardNumber)
        private val tvDefault: TextView = itemView.findViewById(R.id.pmsdk_tvDefault)
        private val tvCardHolder: TextView = itemView.findViewById(R.id.pmsdk_tvCardHolder)
        private val tvExpiry: TextView = itemView.findViewById(R.id.pmsdk_tvExpiry)
        private val ivSelected: ImageView = itemView.findViewById(R.id.pmsdk_ivSelected)
        
        fun bind(method: PaymentMethod, selectedId: Long?) {
            val isSelected = method.id == selectedId
            
            // Card icon
            ivCardIcon.setImageResource(getCardIcon(method.cardType))
            
            // Card details
            tvCardNumber.text = method.maskedNumber
            tvCardHolder.text = method.cardHolderName
            tvExpiry.text = itemView.context.getString(R.string.pmsdk_expires_format, method.expiryDate)
            
            // Default badge
            tvDefault.visibility = if (method.isDefault) View.VISIBLE else View.GONE
            
            // Selection state
            ivSelected.visibility = if (isSelected) View.VISIBLE else View.GONE
            cardView.isSelected = isSelected
            cardView.strokeWidth = if (isSelected) 4 else 2
            cardView.strokeColor = cardView.context.getColor(
                if (isSelected) R.color.pmsdk_primary else R.color.pmsdk_border
            )
            
            // Click listener
            cardView.setOnClickListener {
                onMethodSelected(method)
            }
        }
        
        private fun getCardIcon(cardType: CardType): Int {
            return when (cardType) {
                CardType.VISA -> R.drawable.pmsdk_ic_visa
                CardType.MASTERCARD -> R.drawable.pmsdk_ic_mastercard
                CardType.AMEX -> R.drawable.pmsdk_ic_amex
                CardType.DISCOVER -> R.drawable.pmsdk_ic_card
                CardType.UNKNOWN -> R.drawable.pmsdk_ic_card
            }
        }
    }
    
    class DiffCallback : DiffUtil.ItemCallback<PaymentMethod>() {
        override fun areItemsTheSame(oldItem: PaymentMethod, newItem: PaymentMethod) = 
            oldItem.id == newItem.id
        
        override fun areContentsTheSame(oldItem: PaymentMethod, newItem: PaymentMethod) = 
            oldItem == newItem
    }
}
