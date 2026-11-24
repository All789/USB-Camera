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

# Preserve UVCAndroid & libUVCCamera JNI callbacks and classes
-keep class com.herohan.uvcapp.** { *; }
-keep class com.serenegiant.usb.** { *; }
-keep class com.serenegiant.utils.** { *; }
-keep interface com.serenegiant.usb.IButtonCallback { *; }
-keep interface com.serenegiant.usb.IFrameCallback { *; }
-keep interface com.serenegiant.usb.IStatusCallback { *; }
-dontwarn com.herohan.uvcapp.**
-dontwarn com.serenegiant.**

