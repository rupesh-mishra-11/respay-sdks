package com.paymentmethod.sdk.network

import com.paymentmethod.sdk.models.AddPaymentMethodRequest
import com.paymentmethod.sdk.models.ApiResponse
import com.paymentmethod.sdk.models.PaymentMethod
import com.paymentmethod.sdk.models.PaymentMethodsResponse
import retrofit2.Response
import retrofit2.http.*

/**
 * API service for payment method operations.
 */
interface ApiService {
    
    /**
     * Get all saved payment methods for a customer.
     */
    @GET("customers/{customerId}/payment-methods")
    suspend fun getPaymentMethods(
        @Path("customerId") customerId: Long
    ): Response<ApiResponse<PaymentMethodsResponse>>
    
    /**
     * Add a new payment method.
     */
    @POST("customers/{customerId}/payment-methods")
    suspend fun addPaymentMethod(
        @Path("customerId") customerId: Long,
        @Body request: AddPaymentMethodRequest
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
     * Set a payment method as default.
     */
    @PUT("customers/{customerId}/payment-methods/{methodId}/default")
    suspend fun setDefaultPaymentMethod(
        @Path("customerId") customerId: Long,
        @Path("methodId") methodId: Long
    ): Response<ApiResponse<PaymentMethod>>
}
