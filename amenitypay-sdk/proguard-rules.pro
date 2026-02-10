# AmenityPay SDK ProGuard Rules

# Keep SDK public API
-keep class com.amenitypay.sdk.AmenityPaySDK { *; }
-keep class com.amenitypay.sdk.AmenityPaySDK$* { *; }
-keep class com.amenitypay.sdk.config.SDKConfig { *; }
-keep class com.amenitypay.sdk.config.SDKConfig$* { *; }
-keep class com.amenitypay.sdk.models.** { *; }
-keep class com.amenitypay.sdk.callback.** { *; }

# Retrofit
-keepattributes Signature
-keepattributes Exceptions
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.stream.** { *; }
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
