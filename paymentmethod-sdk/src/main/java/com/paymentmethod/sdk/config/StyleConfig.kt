package com.paymentmethod.sdk.config

import androidx.annotation.ColorInt
import androidx.annotation.StyleRes

/**
 * Style configuration for the PaymentMethod SDK.
 * Host apps can customize the SDK appearance by providing a StyleConfig.
 */
data class StyleConfig(
    /** Primary color for buttons, highlights, etc. */
    @ColorInt val primaryColor: Int? = null,
    
    /** Primary dark color for status bar, etc. */
    @ColorInt val primaryDarkColor: Int? = null,
    
    /** Accent color for secondary actions */
    @ColorInt val accentColor: Int? = null,
    
    /** Background color for screens */
    @ColorInt val backgroundColor: Int? = null,
    
    /** Surface color for cards and dialogs */
    @ColorInt val surfaceColor: Int? = null,
    
    /** Primary text color */
    @ColorInt val textPrimaryColor: Int? = null,
    
    /** Secondary text color */
    @ColorInt val textSecondaryColor: Int? = null,
    
    /** Border color for cards */
    @ColorInt val borderColor: Int? = null,
    
    /** Success color */
    @ColorInt val successColor: Int? = null,
    
    /** Error color */
    @ColorInt val errorColor: Int? = null,
    
    /** Custom theme resource ID (overrides individual colors) */
    @StyleRes val customTheme: Int? = null,
    
    /** Card corner radius in dp */
    val cardCornerRadiusDp: Float? = null,
    
    /** Button corner radius in dp */
    val buttonCornerRadiusDp: Float? = null
) {
    
    /**
     * Builder for StyleConfig.
     */
    class Builder {
        private var primaryColor: Int? = null
        private var primaryDarkColor: Int? = null
        private var accentColor: Int? = null
        private var backgroundColor: Int? = null
        private var surfaceColor: Int? = null
        private var textPrimaryColor: Int? = null
        private var textSecondaryColor: Int? = null
        private var borderColor: Int? = null
        private var successColor: Int? = null
        private var errorColor: Int? = null
        private var customTheme: Int? = null
        private var cardCornerRadiusDp: Float? = null
        private var buttonCornerRadiusDp: Float? = null
        
        fun primaryColor(@ColorInt color: Int) = apply { this.primaryColor = color }
        fun primaryDarkColor(@ColorInt color: Int) = apply { this.primaryDarkColor = color }
        fun accentColor(@ColorInt color: Int) = apply { this.accentColor = color }
        fun backgroundColor(@ColorInt color: Int) = apply { this.backgroundColor = color }
        fun surfaceColor(@ColorInt color: Int) = apply { this.surfaceColor = color }
        fun textPrimaryColor(@ColorInt color: Int) = apply { this.textPrimaryColor = color }
        fun textSecondaryColor(@ColorInt color: Int) = apply { this.textSecondaryColor = color }
        fun borderColor(@ColorInt color: Int) = apply { this.borderColor = color }
        fun successColor(@ColorInt color: Int) = apply { this.successColor = color }
        fun errorColor(@ColorInt color: Int) = apply { this.errorColor = color }
        fun customTheme(@StyleRes themeRes: Int) = apply { this.customTheme = themeRes }
        fun cardCornerRadius(radiusDp: Float) = apply { this.cardCornerRadiusDp = radiusDp }
        fun buttonCornerRadius(radiusDp: Float) = apply { this.buttonCornerRadiusDp = radiusDp }
        
        fun build() = StyleConfig(
            primaryColor = primaryColor,
            primaryDarkColor = primaryDarkColor,
            accentColor = accentColor,
            backgroundColor = backgroundColor,
            surfaceColor = surfaceColor,
            textPrimaryColor = textPrimaryColor,
            textSecondaryColor = textSecondaryColor,
            borderColor = borderColor,
            successColor = successColor,
            errorColor = errorColor,
            customTheme = customTheme,
            cardCornerRadiusDp = cardCornerRadiusDp,
            buttonCornerRadiusDp = buttonCornerRadiusDp
        )
    }
    
    companion object {
        /** Default style configuration */
        val DEFAULT = StyleConfig()
    }
}
