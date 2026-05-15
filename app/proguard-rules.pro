# Hilt
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.HiltAndroidApp

# Room
-keep class androidx.room.** { *; }

# Firebase
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.firebase.** { *; }
-keep class com.uni.colabtasks.data.remote.dto.** { *; }

# Kotlin coroutines
-dontwarn kotlinx.coroutines.**
