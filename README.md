# RP Mobile App SDK

A comprehensive Android SDK suite for payment processing and amenity booking in mobile applications. This project provides three modular SDKs that can be integrated independently or together, depending on your application's needs.

## 📋 Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Features](#features)
- [Requirements](#requirements)
- [Installation](#installation)
- [Quick Start](#quick-start)
- [SDK Documentation](#sdk-documentation)
  - [PaymentMethod SDK](#paymentmethod-sdk)
  - [Payment SDK](#payment-sdk)
  - [AmenityPay SDK](#amenitypay-sdk)
  - [Apple Pay SDK](#apple-pay-sdk)
- [Project Structure](#project-structure)
- [Dependencies](#dependencies)
- [Testing](#testing)
- [Building](#building)
- [License](#license)

## 🎯 Overview

The RP Mobile App SDK is a collection of Android libraries designed to handle payment processing and amenity booking workflows. The SDKs are built with modern Android development practices, using Kotlin, Coroutines, Retrofit, and Material Design components.

### SDK Modules

1. **PaymentMethod SDK** (`paymentmethod-sdk`) - Shared SDK for payment method selection and management
2. **Payment SDK** (`payment-sdk`) - General-purpose payment processing SDK
3. **AmenityPay SDK** (`amenitypay-sdk`) - Specialized SDK for amenity booking and payments (depends on PaymentMethod SDK)
4. **Apple Pay SDK** (`applepay-sdk`) - Apple Pay capability checking and initialization (depends on PaymentMethod SDK)

## 🏗️ Architecture

The project follows a modular architecture where:

- **PaymentMethod SDK** is a shared library used by both Payment SDK and AmenityPay SDK
- Each SDK can be used independently or together
- All SDKs follow a consistent initialization and configuration pattern
- Repository pattern is used for data management
- ViewModel pattern for UI state management
- Network layer uses Retrofit with OkHttp

```
┌─────────────────┐     ┌──────────────┐
│   AmenityPay    │     │  Apple Pay   │
│      SDK        │     │     SDK      │
└────────┬────────┘     └──────┬───────┘
         │                     │
         │ uses                │ uses
         ▼                     ▼
┌─────────────────┐     ┌──────────────┐
│  PaymentMethod  │◄─────┤ Payment SDK  │
│      SDK        │     │              │
└─────────────────┘     └──────────────┘
```

## ✨ Features

### PaymentMethod SDK
- Payment method selection UI
- Payment method management
- Customizable styling
- Mock data support for testing

### Payment SDK
- Simple payment processing flow
- Payment method selection integration
- Transaction management
- Success/failure/cancellation callbacks

### AmenityPay SDK
- Amenity booking workflow
- Time-based booking (start/end datetime)
- Integrated payment processing
- Session management
- Authentication handling

### Apple Pay SDK
- Apple Pay capability checking via BFF API
- Secure API key management (EncryptedSharedPreferences)
- Integration helper for easy setup (`ApplePayIntegrationHelper`)
- Session token management (in-memory only)
- Wallet settings and metadata exposure
- **Note**: Radio button UI is host app's responsibility (SDK provides logic only)
- **Integration**: Integrated into Payment SDK's payment screen (`MakePaymentActivity`)
- **Flow**: Availability check happens on click, not on app startup (better performance)

## 📱 Requirements

- **Minimum SDK**: 24 (Android 7.0)
- **Target SDK**: 36
- **Compile SDK**: 36
- **Java Version**: 11
- **Kotlin**: 2.0.21+
- **Gradle**: 8.13.2+

## 📦 Installation

### Option 1: Local Module (Current Setup)

This project is set up as a multi-module Android project. To use it:

1. Clone the repository:
```bash
git clone <repository-url>
cd rp-mobile-app-sdk
```

2. Open in Android Studio

3. Add the SDK modules to your app's `build.gradle.kts`:
```kotlin
dependencies {
    implementation(project(":paymentmethod-sdk"))
    implementation(project(":payment-sdk"))
    implementation(project(":amenitypay-sdk"))
}
```

### Option 2: AAR Distribution (Future)

Once published, you can add the SDKs as dependencies:
```kotlin
dependencies {
    implementation("com.entrata:paymentmethod-sdk:1.0.0")
    implementation("com.entrata:payment-sdk:1.0.0")
    implementation("com.entrata:amenitypay-sdk:1.0.0")
}
```

## 🚀 Quick Start

### 1. Initialize SDKs in Application Class

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize PaymentMethod SDK (required by other SDKs)
        val paymentMethodConfig = PaymentMethodConfig.Builder()
            .apiKey("your-api-key")
            .environment(PaymentMethodConfig.Environment.DEVELOPMENT)
            .useMockData(true) // Set to false in production
            .enableLogging(true)
            .build()
        
        PaymentMethodSDK.initialize(paymentMethodConfig)
        
        // Initialize Payment SDK
        val paymentConfig = PaymentConfig.builder()
            .apiKey("your-api-key")
            .environment(PaymentEnvironment.DEVELOPMENT)
            .useMockData(false)
            .enableLogging(true)
            .build()
        
        PaymentSDK.initialize(this, paymentConfig)
        
        // Initialize AmenityPay SDK
        val amenityConfig = SDKConfig.builder()
            .baseUrl("https://api.example.com/v1/")
            .subscriptionKey("your-subscription-key")
            .environment(SDKConfig.Environment.DEVELOPMENT)
            .useMockData(false)
            .enableLogging(true)
            .build()
        
        AmenityPaySDK.initialize(this, amenityConfig)
    }
}
```

### 2. Set Customer Credentials

After user login in your app:

```kotlin
// Set for PaymentMethod SDK (shared)
PaymentMethodSDK.setCustomer(
    customerId = customerId,
    authToken = authToken
)

// Set for Payment SDK
PaymentSDK.setCustomer(
    customerId = customerId,
    authToken = authToken
)

// Set for AmenityPay SDK
AmenityPaySDK.setAuthCredentials(
    clientId = clientId,
    propertyId = propertyId,
    customerId = customerId,
    leaseId = leaseId
)
```

## 📚 SDK Documentation

### PaymentMethod SDK

The PaymentMethod SDK provides payment method selection functionality.

#### Usage

```kotlin
// Launch payment method selector
PaymentMethodSDK.selectPaymentMethod(
    activity = this,
    callback = object : PaymentMethodCallback {
        override fun onPaymentMethodSelected(paymentMethod: PaymentMethod) {
            // Handle selected payment method
        }
        
        override fun onCancelled() {
            // Handle cancellation
        }
    }
)
```

#### Configuration

- **API Key**: Your payment method API key
- **Environment**: DEVELOPMENT or PRODUCTION
- **Mock Data**: Enable/disable mock responses
- **Logging**: Enable/disable SDK logging

### Payment SDK

The Payment SDK provides a simple payment processing flow.

#### Usage

```kotlin
val paymentRequest = PaymentRequest(
    amount = 99.99,
    currency = "USD",
    description = "Payment for services"
)

PaymentSDK.makePayment(
    activity = this,
    paymentRequest = paymentRequest,
    callback = object : PaymentCallback {
        override fun onPaymentResult(result: PaymentResult) {
            when (result) {
                is PaymentResult.Success -> {
                    // Payment successful
                    val transactionId = result.transactionId
                    val confirmationNumber = result.confirmationNumber
                }
                is PaymentResult.Failure -> {
                    // Payment failed
                    val errorMessage = result.errorMessage
                }
                is PaymentResult.Cancelled -> {
                    // Payment cancelled by user
                }
            }
        }
    }
)
```

### AmenityPay SDK

The AmenityPay SDK handles amenity booking with integrated payment processing.

#### Usage

```kotlin
val bookingRequest = AmenityBookingRequest(
    amenityId = 100,
    amenityName = "Pool Access",
    amount = 50.00,
    startDatetime = "2024-01-15T10:00:00Z",
    endDatetime = "2024-01-15T12:00:00Z",
    currency = "USD",
    description = "Pool booking for 2 hours"
)

AmenityPaySDK.launchPayment(
    activity = this,
    bookingRequest = bookingRequest,
    requestCode = REQUEST_AMENITY_PAYMENT
)

// Handle result in onActivityResult
override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    if (requestCode == REQUEST_AMENITY_PAYMENT) {
        AmenityPaySDK.handleResult(data, object : AmenityPayCallback {
            override fun onPaymentSuccess(result: AmenityPayResult.Success) {
                // Handle successful booking
            }
            
            override fun onPaymentFailure(result: AmenityPayResult.Failure) {
                // Handle payment failure
            }
            
            override fun onPaymentCancelled() {
                // Handle cancellation
            }
        })
    }
}
```

### Apple Pay SDK

The Apple Pay SDK provides Apple Pay capability checking and initialization via BFF API integration. It's integrated into the Payment SDK's payment screen.

#### Quick Setup

```kotlin
// 1. Store API key in EncryptedSharedPreferences (in Application.onCreate())
val masterKey = MasterKey.Builder(context)
    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
    .build()

val sharedPreferences = EncryptedSharedPreferences.create(
    context,
    "applepay_sdk_secure_prefs",
    masterKey,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
)

sharedPreferences.edit()
    .putString("x_api_key", "your-api-key-here")
    .apply()

// 2. Initialize SDK (in Application.onCreate())
ApplePaySDK.initialize(
    context = applicationContext,
    config = ApplePayConfig.Builder()
        .baseUrl("https://us-residentpay-bff.d05d0001.entratadev.com/")  // Your BFF URL
        .environment(ApplePayConfig.Environment.DEVELOPMENT)
        .enableLogging(true)
        .build(),
    clientType = "mobile-android"
)

// 3. Set session data after user login
ApplePaySDK.setSessionData(
    cid = 12345,
    propertyId = 67890,
    customerId = 11111L,
    leaseId = 22222L
)
```

#### Integration in Payment Screen

The Apple Pay option is integrated into the Payment SDK's `MakePaymentActivity`. When using the Payment SDK, the Apple Pay radio button appears automatically after the payment methods list.

**Recommended Approach**: Use the integration helper which checks availability on click:

```kotlin
// In your payment screen (e.g., MakePaymentActivity)
ApplePayIntegrationHelper.setupApplePayClickHandler(
    activity = this,
    amount = 99.99,
    view = binding.psdkRadioApplePay,  // Your RadioButton view
    callback = object : ApplePayCallback {
        override fun onInitSuccess(sessionToken: String) {
            // Apple Pay initialized - proceed with payment
        }
        
        override fun onFailure(errorCode: String, message: String) {
            // Handle failure
            when (errorCode) {
                "NETWORK_ERROR" -> {
                    // Network connectivity issue
                }
                "APPLE_PAY_UNAVAILABLE" -> {
                    // Apple Pay not available for this payment
                }
            }
        }
        
        override fun onCancelled() {
            // User cancelled
        }
    }
)
```

**Key Points**:
- ✅ Availability check happens **on click**, not on app startup
- ✅ No network calls until user actually selects Apple Pay
- ✅ Radio button UI is host app's responsibility
- ✅ Integrated into Payment SDK's payment screen by default

#### Security Requirements

**IMPORTANT**: The SDK does NOT store API keys. The host app must write the API key to EncryptedSharedPreferences:

```kotlin
// Host app must store API key in EncryptedSharedPreferences
val masterKey = MasterKey.Builder(context)
    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
    .build()

val sharedPreferences = EncryptedSharedPreferences.create(
    context,
    "applepay_sdk_secure_prefs",
    masterKey,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
)

sharedPreferences.edit()
    .putString("x_api_key", "your-api-key")
    .apply()
```

#### UI Integration

**IMPORTANT**: The radio button UI is the host app's responsibility. The Payment SDK includes an Apple Pay radio button in its payment screen layout.

**In Payment SDK Layout** (`psdk_activity_make_payment.xml`):
```xml
<RadioButton
    android:id="@+id/psdk_radioApplePay"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginTop="16dp"
    android:text="Pay with Apple Pay"
    android:visibility="visible" />
```

**Setup in MakePaymentActivity**:
```kotlin
private fun setupApplePay() {
    if (!ApplePaySDK.isInitialized()) {
        binding.psdkRadioApplePay.visibility = View.GONE
        return
    }
    
    val amount = paymentRequest?.amount ?: 0.0
    if (amount <= 0) {
        binding.psdkRadioApplePay.visibility = View.GONE
        return
    }
    
    ApplePayIntegrationHelper.setupApplePayClickHandler(
        activity = this,
        amount = amount,
        view = binding.psdkRadioApplePay,
        callback = applePayCallback
    )
}
```

**Key Points**:
- ✅ No network calls on app startup
- ✅ Availability check happens only when user clicks
- ✅ Radio button appears in Payment SDK's payment screen (after payment methods list)
- ✅ Integrated by default in Payment SDK

For detailed integration instructions, see [Apple Pay SDK Integration Guide](applepay-sdk/INTEGRATION_GUIDE.md).

## 📁 Project Structure

```
rp-mobile-app-sdk/
├── amenitypay-sdk/          # AmenityPay SDK module
│   ├── src/main/java/
│   │   └── com/amenitypay/sdk/
│   │       ├── AmenityPaySDK.kt      # Main SDK entry point
│   │       ├── auth/                 # Authentication
│   │       ├── callback/             # Callback interfaces
│   │       ├── config/               # Configuration
│   │       ├── models/               # Data models
│   │       ├── network/              # Network layer
│   │       ├── repository/           # Data repositories
│   │       └── ui/                   # UI components
│   └── build.gradle.kts
├── payment-sdk/             # Payment SDK module
│   ├── src/main/java/
│   │   └── com/payment/sdk/
│   │       ├── PaymentSDK.kt         # Main SDK entry point
│   │       ├── callback/
│   │       ├── config/
│   │       ├── models/
│   │       ├── network/
│   │       ├── repository/
│   │       └── ui/
│   └── build.gradle.kts
├── paymentmethod-sdk/       # PaymentMethod SDK module (shared)
│   ├── src/main/java/
│   │   └── com/paymentmethod/sdk/
│   │       ├── PaymentMethodSDK.kt   # Main SDK entry point
│   │       ├── callback/
│   │       ├── config/
│   │       ├── models/
│   │       ├── network/
│   │       ├── repository/
│   │       └── ui/
│   └── build.gradle.kts
├── applepay-sdk/            # Apple Pay SDK module
│   ├── src/main/java/
│   │   └── com/applepay/sdk/
│   │       ├── ApplePaySDK.kt        # Main SDK entry point
│   │       ├── ApplePayIntegrationHelper.kt  # Integration helper
│   │       ├── callback/             # Callback interfaces
│   │       ├── config/               # Configuration
│   │       ├── models/               # Data models
│   │       ├── network/              # Network layer (Retrofit, OkHttp)
│   │       ├── repository/           # Data repositories
│   │       ├── security/             # API key management
│   │       └── ui/                   # UI components (deprecated)
│   ├── INTEGRATION_GUIDE.md          # Detailed integration guide
│   ├── TROUBLESHOOTING.md            # Troubleshooting guide
│   ├── ANR_FIX.md                    # ANR fix documentation
│   ├── CHANGES_EXPLAINED.md           # Change history
│   └── build.gradle.kts
├── app/                     # Demo/Test application
│   ├── src/main/java/
│   │   └── com/entrata/rpmobileappsdk/
│   │       ├── SampleApplication.kt  # SDK initialization example
│   │       ├── MainActivity.kt       # Usage examples
│   │       └── AppConstants.kt      # Application constants (BFF URLs, etc.)
│   ├── src/main/res/
│   │   └── xml/
│   │       └── network_security_config.xml  # Network security config
│   └── build.gradle.kts
├── build.gradle.kts         # Root build file
├── settings.gradle.kts      # Project settings
└── gradle/
    └── libs.versions.toml   # Version catalog
```

## 🔧 Dependencies

### Core Dependencies
- **Kotlin**: 2.0.21
- **AndroidX Core KTX**: 1.17.0
- **Material Design**: 1.13.0
- **Lifecycle Components**: 2.9.1

### Networking
- **Retrofit**: 2.11.0
- **OkHttp**: 4.12.0
- **Gson**: 2.11.0

### Coroutines
- **Kotlin Coroutines**: 1.10.2

### Security
- **AndroidX Security Crypto**: 1.1.0-alpha07

### Testing
- **JUnit 5**: 5.10.2
- **MockK**: 1.13.9
- **Turbine**: 1.0.0 (Flow testing)
- **Truth**: 1.4.2
- **Robolectric**: 4.11.1
- **Espresso**: 3.7.0

## 🧪 Testing

### Running Unit Tests

```bash
./gradlew test
```

### Running Instrumentation Tests

```bash
./gradlew connectedAndroidTest
```

### Running Tests for Specific Module

```bash
./gradlew :amenitypay-sdk:test
./gradlew :payment-sdk:test
./gradlew :paymentmethod-sdk:test
```

### Test Coverage

The project uses JUnit 5 for unit testing with MockK for mocking. Each SDK module has comprehensive test coverage including:
- Repository tests
- ViewModel tests
- Network layer tests
- UI component tests

## 🔨 Building

### Build All Modules

```bash
./gradlew build
```

### Build Specific Module

```bash
./gradlew :amenitypay-sdk:assembleRelease
./gradlew :payment-sdk:assembleRelease
./gradlew :paymentmethod-sdk:assembleRelease
```

### Generate AAR Files

```bash
./gradlew :amenitypay-sdk:assembleRelease
./gradlew :payment-sdk:assembleRelease
./gradlew :paymentmethod-sdk:assembleRelease
```

AAR files will be generated in:
- `amenitypay-sdk/build/outputs/aar/`
- `payment-sdk/build/outputs/aar/`
- `paymentmethod-sdk/build/outputs/aar/`

## 🛠️ Development

### Code Style

- Follow Kotlin coding conventions
- Use meaningful variable and function names
- Keep functions small and focused (max 20 lines)
- Follow SOLID principles
- Write unit tests for all new features

### Adding New Features

1. Create feature branch
2. Implement feature with tests
3. Ensure all tests pass
4. Update documentation
5. Create pull request

## 📝 License

[Add your license information here]

## 🤝 Contributing

[Add contribution guidelines here]

## 📞 Support

For issues, questions, or feature requests, please [create an issue](link-to-issues) or contact the development team.

---

**Version**: 1.0.0  
**Last Updated**: 2024
