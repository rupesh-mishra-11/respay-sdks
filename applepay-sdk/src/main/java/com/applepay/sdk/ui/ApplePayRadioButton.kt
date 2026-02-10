package com.applepay.sdk.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.RadioButton
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.applepay.sdk.R
import com.google.android.material.card.MaterialCardView

/**
 * Custom radio button view for Apple Pay option.
 * 
 * @deprecated This UI component is no longer part of the SDK. The host app should create
 * its own radio button UI. When the radio button is clicked, call ApplePaySDK.startApplePay()
 * to initiate the Apple Pay flow.
 * 
 * Usage (new approach):
 * ```xml
 * <RadioButton
 *     android:id="@+id/applePayOption"
 *     android:layout_width="match_parent"
 *     android:layout_height="wrap_content"
 *     android:text="Pay with Apple Pay" />
 * ```
 * 
 * ```kotlin
 * applePayOption.setOnClickListener {
 *     ApplePaySDK.startApplePay(activity, amount, callback)
 * }
 * ```
 */
@Deprecated(
    message = "Radio button UI is now the host app's responsibility. Create your own RadioButton and call ApplePaySDK.startApplePay() when clicked.",
    replaceWith = ReplaceWith("Use a standard RadioButton from your app and call ApplePaySDK.startApplePay() on click")
)
class ApplePayRadioButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    
    private val cardView: MaterialCardView
    private val radioButton: RadioButton
    private val labelTextView: TextView
    
    /**
     * Whether the Apple Pay option is currently selected.
     * Note: This is different from View.isSelected() which is for view selection state.
     */
    var isApplePaySelected: Boolean
        get() = radioButton.isChecked
        set(value) {
            radioButton.isChecked = value
            updateSelectionState(value)
        }
    
    var labelText: String
        get() = labelTextView.text.toString()
        set(value) {
            labelTextView.text = value
        }
    
    init {
        val view = LayoutInflater.from(context).inflate(R.layout.apsdk_item_apple_pay, this, true)
        
        cardView = view.findViewById(R.id.apsdk_cardApplePay)
        radioButton = view.findViewById(R.id.apsdk_radioApplePay)
        labelTextView = view.findViewById(R.id.apsdk_tvApplePayLabel)
        
        // Make entire card clickable
        cardView.setOnClickListener {
            if (!isApplePaySelected) {
                isApplePaySelected = true
                performClick()
            }
        }
        
        // Prevent radio button from being directly clickable (card handles it)
        radioButton.isClickable = false
        radioButton.isFocusable = false
    }
    
    /**
     * Update visual state based on selection.
     */
    private fun updateSelectionState(selected: Boolean) {
        val strokeWidth = if (selected) 4 else 2
        val strokeColor = if (selected) {
            context.getColor(R.color.apsdk_primary)
        } else {
            context.getColor(R.color.apsdk_border)
        }
        
        cardView.strokeWidth = strokeWidth
        cardView.strokeColor = strokeColor
    }
    
    /**
     * Show the Apple Pay option.
     */
    fun show() {
        visibility = View.VISIBLE
    }
    
    /**
     * Hide the Apple Pay option.
     */
    fun hide() {
        visibility = View.GONE
        isApplePaySelected = false
    }
}
