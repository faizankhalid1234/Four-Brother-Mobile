# Fine Trade — release R8 / ProGuard

-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Kotlin / coroutines
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }

# OkHttp / Okio
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Firebase Auth + Realtime Database
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Keep model classes used with JSONObject manually (no reflection needed),
# but keep Parcelable / Serializable just in case.
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}
