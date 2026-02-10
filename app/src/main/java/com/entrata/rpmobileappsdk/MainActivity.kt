package com.entrata.rpmobileappsdk

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.amenitypay.sdk.AmenityPaySDK
import com.amenitypay.sdk.callback.AmenityPayCallback
import com.amenitypay.sdk.models.AmenityBookingRequest
import com.amenitypay.sdk.models.AmenityPayResult
import com.applepay.sdk.ApplePayIntegrationHelper
import com.applepay.sdk.ApplePaySDK
import com.applepay.sdk.callback.ApplePayAvailabilityCallback
import com.applepay.sdk.callback.ApplePayCallback
import com.applepay.sdk.models.ApplePayWalletInfo
import com.entrata.rpmobileappsdk.databinding.ActivityMainBinding
import com.payment.sdk.PaymentSDK
import com.payment.sdk.callback.PaymentCallback
import com.payment.sdk.models.PaymentRequest
import com.payment.sdk.models.PaymentResult
import com.paymentmethod.sdk.PaymentMethodSDK
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Sample MainActivity demonstrating SDK integration
 * 
 * This demo shows how the host app handles authentication and passes
 * the credentials to both SDKs.
 */
class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Simulate: Your app has already authenticated the user
        simulateUserLogin()
        
        setupAmenityCards()
        setupQuickPayButton()
        // Note: Apple Pay integration should be done in the Payment SDK's payment screen,
        // not on the main screen. The radio button should appear after "Select Payment Method"
        // in the Payment SDK's MakePaymentActivity.
    }
    
    /**
     * Simulates your app's login flow.
     */
    private fun simulateUserLogin() {
        val customerId = 11111L
        val authToken = "mock-auth-token-12345"
        
        // Set customer for shared PaymentMethod SDK (used by both SDKs)
        PaymentMethodSDK.setCustomer(
            customerId = customerId,
            authToken = authToken
        )
        
        // Set credentials for AmenityPay SDK
        AmenityPaySDK.setAuthCredentials(
            clientId = 12345,
            propertyId = 67890,
            customerId = customerId,
            leaseId = 22222
        )
        
        // Set customer for Payment SDK
        PaymentSDK.setCustomer(
            customerId = customerId,
            authToken = authToken
        )
        
        // Set session data for Apple Pay SDK
        ApplePaySDK.setSessionData(
            cid = 4547,
            propertyId = 57711,
            customerId = 31723059,
            leaseId = 15456361
        )
    }
    
    private fun setupAmenityCards() {
        // Pool Access Card
        binding.cardPool.setOnClickListener {
            launchAmenityPayment(
                amenityId = 101,
                amenityName = "Pool Access",
                amount = 25.00,
                hours = 2
            )
        }
        
        // Gym Access Card
        binding.cardGym.setOnClickListener {
            launchAmenityPayment(
                amenityId = 102,
                amenityName = "Gym Session",
                amount = 15.00,
                hours = 1
            )
        }
        
        // Tennis Court Card
        binding.cardTennis.setOnClickListener {
            launchAmenityPayment(
                amenityId = 103,
                amenityName = "Tennis Court",
                amount = 40.00,
                hours = 1
            )
        }
        
        // Party Room Card
        binding.cardPartyRoom.setOnClickListener {
            launchAmenityPayment(
                amenityId = 104,
                amenityName = "Party Room Rental",
                amount = 150.00,
                hours = 4
            )
        }
    }
    
    /**
     * Setup the Quick Pay button that uses the new Payment SDK
     */
    private fun setupQuickPayButton() {
        binding.btnQuickPay.setOnClickListener {
            launchQuickPayment(99.99, "Quick Payment Demo")
        }
    }
    
    
    /**
     * Launch payment using the new Payment SDK.
     * This demonstrates a simpler payment flow where you just pass an amount.
     */
    private fun launchQuickPayment(amount: Double, description: String) {
        val paymentRequest = PaymentRequest(
            amount = amount,
            currency = "USD",
            description = description
        )
        
        PaymentSDK.makePayment(this, paymentRequest, object : PaymentCallback {
            override fun onPaymentResult(result: PaymentResult) {
                when (result) {
                    is PaymentResult.Success -> {
                        val message = """
                            Payment Successful!
                            Transaction: ${result.transactionId}
                            Confirmation: ${result.confirmationNumber}
                            Amount: ${NumberFormat.getCurrencyInstance(Locale.US).format(result.amount)}
                        """.trimIndent()
                        Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
                        binding.tvLastBooking.text = "Last Payment: ${result.confirmationNumber}"
                    }
                    is PaymentResult.Failure -> {
                        Toast.makeText(
                            this@MainActivity,
                            "Payment Failed: ${result.errorMessage}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    is PaymentResult.Cancelled -> {
                        Toast.makeText(this@MainActivity, "Payment Cancelled", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }
    
    private fun launchAmenityPayment(
        amenityId: Long,
        amenityName: String,
        amount: Double,
        hours: Int
    ) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.HOUR_OF_DAY, 1)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startDatetime = dateFormat.format(calendar.time)
        
        calendar.add(Calendar.HOUR_OF_DAY, hours)
        val endDatetime = dateFormat.format(calendar.time)
        
        val bookingRequest = AmenityBookingRequest(
            amenityId = amenityId,
            amenityName = amenityName,
            amount = amount,
            startDatetime = startDatetime,
            endDatetime = endDatetime,
            currency = "USD",
            description = "Booking for $amenityName"
        )
        
        AmenityPaySDK.launchPayment(this, bookingRequest, REQUEST_AMENITY_PAYMENT)
    }
    
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == REQUEST_AMENITY_PAYMENT) {
            AmenityPaySDK.handleResult(data, object : AmenityPayCallback {
                override fun onPaymentSuccess(result: AmenityPayResult.Success) {
                    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
                    val message = """
                        Payment Successful!
                        Transaction: ${result.transactionId}
                        Confirmation: ${result.confirmationNumber}
                        Amount: ${currencyFormat.format(result.amount)}
                    """.trimIndent()
                    
                    Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
                    binding.tvLastBooking.text = "Last Booking: ${result.confirmationNumber}"
                }
                
                override fun onPaymentFailure(result: AmenityPayResult.Failure) {
                    Toast.makeText(
                        this@MainActivity,
                        "Payment Failed: ${result.errorMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                }
                
                override fun onPaymentCancelled() {
                    Toast.makeText(
                        this@MainActivity,
                        "Payment Cancelled",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
        }
    }
    
    companion object {
        private const val REQUEST_AMENITY_PAYMENT = 100
    }
}
