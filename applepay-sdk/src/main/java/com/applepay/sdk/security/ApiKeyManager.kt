package com.applepay.sdk.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.applepay.sdk.ApplePaySDK

/**
 * Manages reading API key from EncryptedSharedPreferences.
 * 
 * Security: The host app must write the API key to EncryptedSharedPreferences
 * using the key defined in KEY_PREF_NAME and KEY_API_KEY.
 * 
 * The SDK only reads, never writes.
 * 
 * Performance: EncryptedSharedPreferences instance is cached to avoid blocking
 * operations on the main thread during network requests.
 */
internal object ApiKeyManager {
    
    private const val KEY_PREF_NAME = "applepay_sdk_secure_prefs"
    private const val KEY_API_KEY = "x_api_key"
    
    // Cache the EncryptedSharedPreferences instance to avoid repeated creation
    // which can block the main thread, especially on first access
    @Volatile
    private var cachedPreferences: SharedPreferences? = null
    
    @Volatile
    private var cachedContext: Context? = null
    
    /**
     * Read API key from EncryptedSharedPreferences.
     * 
     * This method caches the EncryptedSharedPreferences instance to avoid
     * blocking operations on subsequent calls.
     * 
     * @param context Application context
     * @return API key if found, null otherwise
     */
    fun getApiKey(context: Context): String? {
        return try {
            val appContext = context.applicationContext
            
            // Use cached instance if available and context matches
            val prefs = cachedPreferences?.takeIf { 
                cachedContext === appContext 
            } ?: createEncryptedPreferences(appContext)
            
            prefs.getString(KEY_API_KEY, null)
        } catch (e: Exception) {
            // Log error but don't expose sensitive information
            if (ApplePaySDK.isLoggingEnabled()) {
                android.util.Log.e("ApplePaySDK", "Failed to read API key from secure storage", e)
            }
            null
        }
    }
    
    /**
     * Create and cache EncryptedSharedPreferences instance.
     * This is a potentially blocking operation, so it's done once and cached.
     */
    private fun createEncryptedPreferences(context: Context): SharedPreferences {
        // Double-check locking pattern to ensure thread safety
        synchronized(this) {
            // Check again after acquiring lock
            cachedPreferences?.takeIf { cachedContext === context }?.let {
                return it
            }
            
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            
            val sharedPreferences = EncryptedSharedPreferences.create(
                context,
                KEY_PREF_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            
            // Cache the instance
            cachedPreferences = sharedPreferences
            cachedContext = context
            
            return sharedPreferences
        }
    }
    
    /**
     * Check if API key is available.
     */
    fun hasApiKey(context: Context): Boolean {
        return getApiKey(context) != null
    }
    
    /**
     * Clear cached preferences (useful for testing or when context changes).
     */
    internal fun clearCache() {
        synchronized(this) {
            cachedPreferences = null
            cachedContext = null
        }
    }
}
