package com.amenitypay.sdk.network

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.util.UUID

/**
 * Mock interceptor that returns dummy responses for testing
 */
class MockInterceptor : Interceptor {
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath
        val method = request.method
        
        // Simulate network delay
        Thread.sleep(500)
        
        val (responseCode, responseBody) = when {
            // Get customer profile
            path.contains("customers") && path.contains("payment-accounts").not() 
                    && path.contains("payment-history").not() && method == "GET" -> {
                200 to mockCustomerProfileResponse()
            }
            
            // Get payment accounts
            path.contains("payment-accounts") && method == "GET" -> {
                200 to mockPaymentAccountsResponse()
            }
            
            // Add payment account
            path.contains("payment-accounts") && method == "POST" -> {
                201 to mockAddPaymentAccountResponse()
            }
            
            // Delete payment account
            path.contains("payment-accounts") && method == "DELETE" -> {
                200 to mockDeleteResponse()
            }
            
            // Set default payment account
            path.contains("payment-accounts") && path.contains("default") && method == "PUT" -> {
                200 to mockSetDefaultPaymentAccountResponse()
            }
            
            // Process payment
            path.contains("amenity-payments") && method == "POST" -> {
                200 to mockProcessPaymentResponse()
            }
            
            // Payment history
            path.contains("payment-history") && method == "GET" -> {
                200 to mockPaymentHistoryResponse()
            }
            
            else -> {
                404 to """{"success": false, "error": {"code": "NOT_FOUND", "message": "Endpoint not found"}}"""
            }
        }
        
        return Response.Builder()
            .code(responseCode)
            .message("OK")
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .body(responseBody.toResponseBody("application/json".toMediaType()))
            .build()
    }
    
    private fun mockCustomerProfileResponse(): String {
        return """
        {
            "success": true,
            "data": {
                "customer_id": 11111,
                "client_id": 12345,
                "property_id": 67890,
                "lease_id": 22222,
                "first_name": "John",
                "last_name": "Doe",
                "email": "john.doe@example.com",
                "phone": "+1-555-123-4567"
            },
            "error": null
        }
        """.trimIndent()
    }
    
    private fun mockPaymentAccountsResponse(): String {
        return """
        {
            "success": true,
            "data": {
                "payment_accounts": [
                    {
                        "customer_payment_account_id": 1001,
                        "payment_type_id": 1,
                        "payment_type_name": "Credit Card",
                        "card_type": "visa",
                        "last_four": "4242",
                        "expiry_month": 12,
                        "expiry_year": 2027,
                        "card_holder_name": "John Doe",
                        "is_default": true,
                        "is_active": true
                    },
                    {
                        "customer_payment_account_id": 1002,
                        "payment_type_id": 1,
                        "payment_type_name": "Credit Card",
                        "card_type": "mastercard",
                        "last_four": "5555",
                        "expiry_month": 6,
                        "expiry_year": 2026,
                        "card_holder_name": "John Doe",
                        "is_default": false,
                        "is_active": true
                    },
                    {
                        "customer_payment_account_id": 1003,
                        "payment_type_id": 2,
                        "payment_type_name": "Debit Card",
                        "card_type": "amex",
                        "last_four": "1234",
                        "expiry_month": 3,
                        "expiry_year": 2028,
                        "card_holder_name": "John D",
                        "is_default": false,
                        "is_active": true
                    }
                ],
                "total_count": 3
            },
            "error": null
        }
        """.trimIndent()
    }
    
    private fun mockAddPaymentAccountResponse(): String {
        return """
        {
            "success": true,
            "data": {
                "customer_payment_account_id": ${System.currentTimeMillis() % 10000},
                "payment_type_id": 1,
                "payment_type_name": "Credit Card",
                "card_type": "visa",
                "last_four": "1111",
                "expiry_month": 12,
                "expiry_year": 2028,
                "card_holder_name": "John Doe",
                "is_default": false,
                "is_active": true
            },
            "error": null
        }
        """.trimIndent()
    }
    
    private fun mockDeleteResponse(): String {
        return """
        {
            "success": true,
            "data": null,
            "error": null
        }
        """.trimIndent()
    }
    
    private fun mockSetDefaultPaymentAccountResponse(): String {
        return """
        {
            "success": true,
            "data": {
                "customer_payment_account_id": 1001,
                "payment_type_id": 1,
                "payment_type_name": "Credit Card",
                "card_type": "visa",
                "last_four": "4242",
                "expiry_month": 12,
                "expiry_year": 2027,
                "card_holder_name": "John Doe",
                "is_default": true,
                "is_active": true
            },
            "error": null
        }
        """.trimIndent()
    }
    
    private fun mockProcessPaymentResponse(): String {
        val transactionId = "TXN-${UUID.randomUUID().toString().take(8).uppercase()}"
        val confirmationNumber = "CNF-${System.currentTimeMillis()}"
        
        return """
        {
            "success": true,
            "data": {
                "transaction_id": "$transactionId",
                "status": "success",
                "amount": 150.00,
                "currency": "USD",
                "amenity_id": 100,
                "customer_payment_account_id": 1001,
                "created_at": "${java.time.Instant.now()}",
                "confirmation_number": "$confirmationNumber",
                "message": "Payment processed successfully"
            },
            "error": null
        }
        """.trimIndent()
    }
    
    private fun mockPaymentHistoryResponse(): String {
        return """
        {
            "success": true,
            "data": [
                {
                    "transaction_id": "TXN-ABC12345",
                    "status": "success",
                    "amount": 75.00,
                    "currency": "USD",
                    "amenity_id": 101,
                    "customer_payment_account_id": 1001,
                    "created_at": "2024-01-10T14:30:00Z",
                    "confirmation_number": "CNF-1704899400000",
                    "message": "Pool reservation payment"
                },
                {
                    "transaction_id": "TXN-DEF67890",
                    "status": "success",
                    "amount": 120.00,
                    "currency": "USD",
                    "amenity_id": 102,
                    "customer_payment_account_id": 1002,
                    "created_at": "2024-01-05T10:00:00Z",
                    "confirmation_number": "CNF-1704448800000",
                    "message": "Gym access payment"
                }
            ],
            "error": null
        }
        """.trimIndent()
    }
}
