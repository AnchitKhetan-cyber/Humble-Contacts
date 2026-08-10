# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ─────────────────────────────────────────────────────────────────────────────
# Keep rules for release minification (R8). See docs/release-hardening.md.
# ─────────────────────────────────────────────────────────────────────────────

# Firestore data models are populated by REFLECTION: toObject(Contact::class.java)
# maps Firestore field names to Kotlin property names. If R8 renames or removes
# these properties/constructors, deserialization silently drops fields. Keep the
# whole model package (classes, members, and the no-arg + default-value
# constructors Firestore needs). The classes also carry @Keep as documentation.
-keep class com.humblesolutions.humblecontacts.data.model.** { *; }
-keepclassmembers class com.humblesolutions.humblecontacts.data.model.** {
    <init>(...);
    <fields>;
}

# Honour androidx @Keep anywhere it's applied.
-keep @androidx.annotation.Keep class * { *; }
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}

# Firestore annotations used on the models (@Exclude, @PropertyName) must survive
# so the mapper reads them; keep annotation attributes generally.
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# Kotlin metadata — helps reflective/Firestore mapping resolve Kotlin types.
-keep class kotlin.Metadata { *; }

# kotlinx.serialization: keep generated serializers for any @Serializable type
# (ContactInfo is annotated; defensive even though it's mapped manually today).
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class *
-keep class <1> { *; }

# Firebase, ML Kit (text recognition) and zxing ship their own consumer keep
# rules via their AARs, so their internals are retained automatically. No extra
# rules needed here unless a runtime R8 warning proves otherwise.