package com.amenitypay.sdk.network

import com.amenitypay.sdk.models.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit API Service interface for AmenityPay SDK
 */
interface ApiService {
    
    // ==================== Customer Profile ====================
    
    /**
     * Get customer profile by credentials
     */
    @GET("customers/{customerId}")
    suspend fun getCustomerProfile(
        @Path("customerId") customerId: Long,
        @Query("client_id") clientId: Long,
        @Query("property_id") propertyId: Long,
        @Query("lease_id") leaseId: Long
    ): Response<ApiResponse<CustomerProfile>>
    
    // ==================== Payment Accounts ====================
    
    /**
     * Get all saved payment accounts for the customer
     */
    @GET("customers/{customerId}/payment-accounts")
    suspend fun getPaymentAccounts(
        @Path("customerId") customerId: Long,
        @Query("client_id") clientId: Long,
        @Query("property_id") propertyId: Long
    ): Response<ApiResponse<CustomerPaymentAccountsResponse>>
    
    /**
     * Add a new payment account
     */
    @POST("customers/{customerId}/payment-accounts")
    suspend fun addPaymentAccount(
        @Path("customerId") customerId: Long,
        @Body request: AddPaymentAccountRequest
    ): Response<ApiResponse<CustomerPaymentAccount>>
    
    /**
     * Delete a payment account
     */
    @DELETE("customers/{customerId}/payment-accounts/{accountId}")
    suspend fun deletePaymentAccount(
        @Path("customerId") customerId: Long,
        @Path("accountId") accountId: Long
    ): Response<ApiResponse<Unit>>
    
    /**
     * Set a payment account as default
     */
    @PUT("customers/{customerId}/payment-accounts/{accountId}/default")
    suspend fun setDefaultPaymentAccount(
        @Path("customerId") customerId: Long,
        @Path("accountId") accountId: Long
    ): Response<ApiResponse<CustomerPaymentAccount>>
    
    // ==================== Amenity Payments ====================
    
    /**
     * Process amenity payment
     */
    @POST("amenity-payments")
    suspend fun processPayment(
        @Body request: ProcessPaymentRequest
    ): Response<ApiResponse<PaymentResponse>>
    
    /**
     * Get payment history for customer
     */
    @GET("customers/{customerId}/payment-history")
    suspend fun getPaymentHistory(
        @Path("customerId") customerId: Long,
        @Query("client_id") clientId: Long,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<ApiResponse<List<PaymentResponse>>>
}
