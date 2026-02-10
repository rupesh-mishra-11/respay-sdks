# ProGuard rules for ApplePay SDK

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

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Retrofit
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
