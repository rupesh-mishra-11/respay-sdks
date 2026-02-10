# PaymentMethod SDK ProGuard Rules

# Keep public SDK classes
-keep public class com.paymentmethod.sdk.** { public *; }

# Keep models
-keep class com.paymentmethod.sdk.models.** { *; }

# Keep callbacks
-keep interface com.paymentmethod.sdk.callback.** { *; }

# Keep Gson serialization
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }

# OkHttp
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }

# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
