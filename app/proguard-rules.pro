# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.migux97.simon.**$$serializer { *; }
-keepclassmembers class com.migux97.simon.** {
    *** Companion;
}
-keepclasseswithmembers class com.migux97.simon.** {
    kotlinx.serialization.KSerializer serializer(...);
}
