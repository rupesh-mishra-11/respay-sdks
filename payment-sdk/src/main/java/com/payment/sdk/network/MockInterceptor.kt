package com.payment.sdk.network

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

/**
 * Mock interceptor that returns fake API responses for testing.
 */
class MockInterceptor : Interceptor {
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath
        val method = request.method
        
        Thread.sleep(500) // Simulate network delay
        
        val mockResponse = when {
            path.contains("/payment-methods") && method == "GET" -> mockGetPaymentMethods()
            path.contains("/payment-methods") && method == "POST" -> mockAddPaymentMethod()
            path.contains("/payment-methods") && method == "DELETE" -> mockDeletePaymentMethod()
            path.contains("/payments/process") && method == "POST" -> mockProcessPayment()
            path.contains("/api/validate") && method == "POST" -> mockValidatePayment()
            path.contains("/api/finalize") && method == "POST" -> mockFinalizePayment()
            else -> createResponse(404, """{"success":false,"error":{"code":"NOT_FOUND","message":"Endpoint not found"}}""")
        }
        
        return mockResponse.newBuilder().request(request).build()
    }
    
    private fun mockGetPaymentMethods(): Response {
        val json = """
        {
            "success": true,
            "data": [
                {
                    "id": 1001,
                    "card_type": "visa",
                    "last_four": "4242",
                    "expiry_month": 12,
                    "expiry_year": 2027,
                    "card_holder_name": "John Doe",
                    "is_default": true
                },
                {
                    "id": 1002,
                    "card_type": "mastercard",
                    "last_four": "5555",
                    "expiry_month": 6,
                    "expiry_year": 2026,
                    "card_holder_name": "John Doe",
                    "is_default": false
                },
                {
                    "id": 1003,
                    "card_type": "amex",
                    "last_four": "1234",
                    "expiry_month": 3,
                    "expiry_year": 2028,
                    "card_holder_name": "John D",
                    "is_default": false
                }
            ],
            "error": null
        }
        """.trimIndent()
        
        return createResponse(200, json)
    }
    
    private fun mockAddPaymentMethod(): Response {
        val json = """
        {
            "success": true,
            "data": {
                "id": 1004,
                "card_type": "visa",
                "last_four": "9999",
                "expiry_month": 10,
                "expiry_year": 2028,
                "card_holder_name": "New Card",
                "is_default": false
            },
            "error": null
        }
        """.trimIndent()
        
        return createResponse(200, json)
    }
    
    private fun mockDeletePaymentMethod(): Response {
        val json = """
        {
            "success": true,
            "data": null,
            "error": null
        }
        """.trimIndent()
        
        return createResponse(200, json)
    }
    
    private fun mockProcessPayment(): Response {
        val json = """
        {
            "success": true,
            "data": {
                "transaction_id": "TXN-${System.currentTimeMillis()}",
                "status": "success",
                "amount": 99.99,
                "currency": "USD",
                "confirmation_number": "CNF-${(100000..999999).random()}",
                "message": "Payment processed successfully"
            },
            "error": null
        }
        """.trimIndent()
        
        return createResponse(200, json)
    }
    
    private fun mockValidatePayment(): Response {
        val json = """
        {
            "success": true,
            "statusCode": 200,
            "data": {
                "validated": true,
                "transaction_id": "TXN-VALIDATE-${System.currentTimeMillis()}"
            },
            "error": null
        }
        """.trimIndent()
        
        return createResponse(200, json)
    }
    
    private fun mockFinalizePayment(): Response {
        val json = """
        {
            "success": true,
            "statusCode": 200,
            "data": {
                "transaction_id": "TXN-FINALIZE-${System.currentTimeMillis()}",
                "confirmation_number": "CNF-${(100000..999999).random()}",
                "amount": 99.99,
                "currency": "USD"
            },
            "error": null
        }
        """.trimIndent()
        
        return createResponse(200, json)
    }
    
    private fun createResponse(code: Int, body: String): Response {
        return Response.Builder()
            .request(okhttp3.Request.Builder().url("http://mock").build())
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message(if (code == 200) "OK" else "Error")
            .body(body.toResponseBody("application/json".toMediaType()))
            .build()
    }
}
