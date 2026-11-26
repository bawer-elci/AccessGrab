# AccessGrab Kod Koruma & Karıştırma (ProGuard & R8 Obfuscation)
# Uygulama kodlarını ve mimarisini tersine mühendisliğe karşı tamamen harflere (a, b, c) dönüştürür.

-repackageclasses ''
-allowaccessmodification
-renamesourcefileattribute ''
-keepattributes SourceFile,LineNumberTable

# Kotlin, Coroutines ve Reflection İçin Zorunlu Nitelikler
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes *Annotation*
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations

# Retrofit 2 & OkHttp Kuralları
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-dontwarn javax.annotation.**
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# AccessGrab Ağ Servisleri ve Veri Modelleri (Retrofit ve Gson'un çalışması için zorunludur)
-keep class com.baverelci.accessgrab.network.** { *; }
-keepclassmembers class com.baverelci.accessgrab.network.** { *; }

# Gson Serileştirme Alanları
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Android Zorunlu Sistem Bileşenleri
-keep public class com.baverelci.accessgrab.MainActivity
-keep public class androidx.core.content.FileProvider
-keep class com.baverelci.accessgrab.notification.** { *; }
-keepclassmembers class com.baverelci.accessgrab.notification.** { *; }
