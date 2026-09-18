-keepattributes *Annotation*,Signature,Exceptions,InnerClasses,EnclosingMethod,LineNumberTable

-keep class kotlin.Metadata { *; }
-keep class kotlin.coroutines.Continuation { *; }
-keepclassmembers class kotlin.coroutines.Continuation { *; }
-keepnames class kotlinx.coroutines.internal.CoroutineExceptionHandlerImpl { *; }

-keep class com.google.gson.** { *; }
-keep class * extends com.google.gson.reflect.TypeToken { *; }
-keepclassmembers class * extends com.google.gson.reflect.TypeToken { <fields>; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep,allowobfuscation class * extends com.google.gson.TypeAdapter
-keep,allowobfuscation class * implements com.google.gson.TypeAdapterFactory
-keep,allowobfuscation class * implements com.google.gson.JsonSerializer
-keep,allowobfuscation class * implements com.google.gson.JsonDeserializer

-keep class retrofit2.** { *; }
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-keep,allowobfuscation,allowshrinking class retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

-keep class okhttp3.** { *; }
-keep class okio.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

-keep class coil.** { *; }
-keepclassmembers class coil.** { *; }
-dontwarn coil.**

-keep class com.gxjzy.huizhijiao.** { *; }
-keepclassmembers class com.gxjzy.huizhijiao.** { *; }
-keepnames class com.gxjzy.huizhijiao.model.** { *; }

-keepclassmembers class * {
    @androidx.annotation.Keep *;
}
-keep @androidx.annotation.Keep class * { *; }

-keep class java.util.LinkedHashMap { *; }
-keep class java.util.HashMap { *; }
-keep class java.util.ArrayList { *; }

-dontwarn com.google.android.gms.**
-dontwarn kotlin.**
-dontwarn javax.annotation.**
