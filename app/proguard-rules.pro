# Keep Room entities
-keep class com.adshield.detector.data.** { *; }
-keepclassmembers class com.adshield.detector.data.** { *; }

# --- androidx.security-crypto (Google Tink) pulls in annotation-only
# libraries (error_prone_annotations, javax.annotation, jsr305) that are
# compile-time-only and aren't shipped on the classpath at runtime. R8's
# missing-class check blocks the build over these unless told to ignore
# them; this is the standard, safe fix (not a functional risk).
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
-dontwarn javax.annotation.concurrent.**
-dontwarn org.checkerframework.**
-dontwarn com.google.j2objc.annotations.**

# Keep Tink's runtime reflection-based key/proto handling intact.
-keep class com.google.crypto.tink.** { *; }
-keepclassmembers class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**

# SQLCipher / SQLite native bindings
-keep class net.sqlcipher.** { *; }
-keepclassmembers class net.sqlcipher.** { *; }
-dontwarn net.sqlcipher.**
