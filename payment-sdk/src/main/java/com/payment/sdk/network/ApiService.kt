package com.payment.sdk.network

import com.payment.sdk.models.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit API interface for payment endpoints.
 */
interface ApiService {
    
    /**
     * Get saved payment methods for a customer.
     */
    @GET("customers/{customerId}/payment-methods")
    suspend fun getPaymentMethods(
        @Path("customerId") customerId: Long
    ): Response<ApiResponse<List<PaymentMethod>>>
    
    /**
     * Add a new payment method.
     */
    @POST("customers/{customerId}/payment-methods")
    suspend fun addPaymentMethod(
        @Path("customerId") customerId: Long,
        @Body request: AddCardRequest
    ): Response<ApiResponse<PaymentMethod>>
    
    /**
     * Delete a payment method.
     */
    @DELETE("customers/{customerId}/payment-methods/{methodId}")
    suspend fun deletePaymentMethod(
        @Path("customerId") customerId: Long,
        @Path("methodId") methodId: Long
    ): Response<ApiResponse<Unit>>
    
    /**
     * Process a payment.
     */
    @POST("payments/process")
    suspend fun processPayment(
        @Body request: ProcessPaymentRequest
    ): Response<ApiResponse<PaymentTransaction>>
    
    /**
     * Validate payment.
     */
    @POST("api/validate")
    suspend fun validatePayment(
        @Body request: ValidateRequest
    ): Response<ValidateResponse>
    
    /**
     * Finalize payment.
     */
    @POST("api/finalize")
    suspend fun finalizePayment(
        @Body request: FinalizeRequest
    ): Response<FinalizeResponse>
}
