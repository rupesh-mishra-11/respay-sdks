package com.applepay.sdk.network

import com.applepay.sdk.models.ApplePayInitRequest
import com.applepay.sdk.models.ApplePayInitResponse
import com.applepay.sdk.models.ApplePayValidateRequest
import com.applepay.sdk.models.ApplePayValidateResponse
import com.applepay.sdk.models.ApplePayFinalizeRequest
import com.applepay.sdk.models.ApplePayFinalizeResponse
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit service interface for Apple Pay BFF API.
 */
internal interface ApiService {
    
    /**
     * Initialize Apple Pay and check availability.
     * 
     * POST /api/init
     */
    @POST("api/init")
    suspend fun initializeApplePay(
        @Body request: ApplePayInitRequest
    ): ApplePayInitResponse
    
    /**
     * Validate Apple Pay payment data.
     * 
     * POST /api/validate
     * Requires x-session-token header.
     */
    @POST("api/validate")
    suspend fun validateApplePay(
        @Body request: ApplePayValidateRequest
    ): ApplePayValidateResponse
    
    /**
     * Finalize Apple Pay payment.
     * 
     * POST /api/finalize
     * Requires x-session-token header.
     */
    @POST("api/finalize")
    suspend fun finalizeApplePay(
        @Body request: ApplePayFinalizeRequest
    ): ApplePayFinalizeResponse
}
