package com.entrata.rpmobileappsdk

/**
 * Application-wide constants.
 * 
 * Centralized configuration values for the app.
 */
object AppConstants {
    
    /**
     * Apple Pay BFF base URL.
     * 
     * This is the backend-for-frontend (BFF) API endpoint for Apple Pay functionality.
     * Update this value based on your environment (development, staging, production).
     */
    const val APPLE_PAY_BFF_BASE_URL = "https://us-residentpay-bff.d05d0001.entratadev.com/"
    
    /**
     * Apple Pay Client Type.
     * 
     * Sent as x-client-type header to BFF.
     */
    const val APPLE_PAY_CLIENT_TYPE = "mobile-android"
    const val BFF_PUBLISABLE_KEY = "Nkhb6LSJJI6wZvCb43zMQ7FfX8zLdbHE8TYRQYbo"
}
