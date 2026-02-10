package com.amenitypay.sdk.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.amenitypay.sdk.models.AuthCredentials

/**
 * Manages secure storage and retrieval of session credentials
 */
class SessionManager(context: Context) {
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val securePrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    /**
     * Save authentication credentials securely
     */
    fun saveCredentials(credentials: AuthCredentials) {
        securePrefs.edit().apply {
            putLong(KEY_CLIENT_ID, credentials.clientId)
            putLong(KEY_PROPERTY_ID, credentials.propertyId)
            putLong(KEY_CUSTOMER_ID, credentials.customerId)
            putLong(KEY_LEASE_ID, credentials.leaseId)
            putLong(KEY_CREDENTIALS_SAVED_AT, System.currentTimeMillis())
            apply()
        }
    }
    
    /**
     * Get stored credentials
     */
    fun getCredentials(): AuthCredentials? {
        val clientId = securePrefs.getLong(KEY_CLIENT_ID, -1)
        val propertyId = securePrefs.getLong(KEY_PROPERTY_ID, -1)
        val customerId = securePrefs.getLong(KEY_CUSTOMER_ID, -1)
        val leaseId = securePrefs.getLong(KEY_LEASE_ID, -1)
        
        // Return null if any required field is missing
        if (clientId == -1L || propertyId == -1L || customerId == -1L || leaseId == -1L) {
            return null
        }
        
        return AuthCredentials(
            clientId = clientId,
            propertyId = propertyId,
            customerId = customerId,
            leaseId = leaseId
        )
    }
    
    /**
     * Get client ID
     */
    fun getClientId(): Long = securePrefs.getLong(KEY_CLIENT_ID, -1)
    
    /**
     * Get property ID
     */
    fun getPropertyId(): Long = securePrefs.getLong(KEY_PROPERTY_ID, -1)
    
    /**
     * Get customer ID
     */
    fun getCustomerId(): Long = securePrefs.getLong(KEY_CUSTOMER_ID, -1)
    
    /**
     * Get lease ID
     */
    fun getLeaseId(): Long = securePrefs.getLong(KEY_LEASE_ID, -1)
    
    /**
     * Check if valid credentials are stored
     */
    fun hasValidCredentials(): Boolean {
        return getCredentials() != null
    }
    
    /**
     * Clear all stored credentials (logout)
     */
    fun clearSession() {
        securePrefs.edit().apply {
            remove(KEY_CLIENT_ID)
            remove(KEY_PROPERTY_ID)
            remove(KEY_CUSTOMER_ID)
            remove(KEY_LEASE_ID)
            remove(KEY_CREDENTIALS_SAVED_AT)
            apply()
        }
    }
    
    companion object {
        private const val PREFS_NAME = "amenitypay_session_prefs"
        private const val KEY_CLIENT_ID = "client_id"
        private const val KEY_PROPERTY_ID = "property_id"
        private const val KEY_CUSTOMER_ID = "customer_id"
        private const val KEY_LEASE_ID = "lease_id"
        private const val KEY_CREDENTIALS_SAVED_AT = "credentials_saved_at"
    }
}
