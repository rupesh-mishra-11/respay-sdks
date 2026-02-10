# Consumer ProGuard rules for ApplePay SDK
# These rules are applied to apps that consume this SDK

# Keep SDK public API
-keep class com.applepay.sdk.ApplePaySDK { *; }
-keep class com.applepay.sdk.config.** { *; }
-keep class com.applepay.sdk.callback.** { *; }
-keep class com.applepay.sdk.models.** { *; }

# Keep Retrofit interfaces
-keep interface com.applepay.sdk.network.** { *; }

# Keep Gson models
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.applepay.sdk.models.** { *; }
