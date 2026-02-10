package com.paymentmethod.sdk.network

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

/**
 * Interceptor for providing mock API responses.
 */
class MockInterceptor : Interceptor {
    
    companion object {
        private val paymentMethods = mutableListOf(
            MockPaymentMethod(1, "visa", "4242", 12, 2027, "John Doe", true),
            MockPaymentMethod(2, "mastercard", "5555", 6, 2026, "John Doe", false),
            MockPaymentMethod(3, "amex", "0005", 3, 2028, "John D", false)
        )
        private var nextId = 4L
    }
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath
        val method = request.method
        
        // Simulate network delay
        Thread.sleep(500)
        
        val responseBody = when {
            path.contains("/payment-methods") && method == "GET" -> getPaymentMethodsResponse()
            path.contains("/payment-methods") && method == "POST" -> addPaymentMethodResponse()
            path.contains("/payment-methods") && method == "DELETE" -> deletePaymentMethodResponse()
            path.contains("/default") && method == "PUT" -> setDefaultResponse(path)
            else -> """{"success": false, "error": {"code": "NOT_FOUND", "message": "Endpoint not found"}}"""
        }
        
        return Response.Builder()
            .code(200)
            .message("OK")
            .protocol(Protocol.HTTP_1_1)
            .request(request)
            .body(responseBody.toResponseBody("application/json".toMediaType()))
            .build()
    }
    
    private fun getPaymentMethodsResponse(): String {
        val methods = paymentMethods.joinToString(",") { it.toJson() }
        return """
        {
            "success": true,
            "data": {
                "payment_methods": [$methods],
                "total_count": ${paymentMethods.size}
            }
        }
        """.trimIndent()
    }
    
    private fun addPaymentMethodResponse(): String {
        val newMethod = MockPaymentMethod(
            id = nextId++,
            cardType = "visa",
            lastFour = "1234",
            expiryMonth = 12,
            expiryYear = 2028,
            cardHolderName = "New Card",
            isDefault = false
        )
        paymentMethods.add(newMethod)
        
        return """
        {
            "success": true,
            "data": ${newMethod.toJson()}
        }
        """.trimIndent()
    }
    
    private fun deletePaymentMethodResponse(): String {
        return """{"success": true, "data": null}"""
    }
    
    private fun setDefaultResponse(path: String): String {
        val methodId = path.split("/").dropLast(1).lastOrNull()?.toLongOrNull() ?: 1L
        
        // Update default status
        paymentMethods.forEach { it.isDefault = it.id == methodId }
        
        val method = paymentMethods.find { it.id == methodId }
        return if (method != null) {
            """{"success": true, "data": ${method.toJson()}}"""
        } else {
            """{"success": false, "error": {"code": "NOT_FOUND", "message": "Payment method not found"}}"""
        }
    }
    
    private data class MockPaymentMethod(
        val id: Long,
        val cardType: String,
        val lastFour: String,
        val expiryMonth: Int,
        val expiryYear: Int,
        val cardHolderName: String,
        var isDefault: Boolean
    ) {
        fun toJson(): String = """
        {
            "id": $id,
            "payment_type_id": 1,
            "card_type": "$cardType",
            "last_four": "$lastFour",
            "expiry_month": $expiryMonth,
            "expiry_year": $expiryYear,
            "card_holder_name": "$cardHolderName",
            "is_default": $isDefault,
            "is_active": true
        }
        """.trimIndent()
    }
}
