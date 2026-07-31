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

# Mantieni file e numeri di riga per stack trace leggibili dopo il deoffuscamento
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# --- JavaMail (com.sun.mail:android-mail / android-activation) ---
# JavaMail carica provider di trasporto e handler MIME via reflection
-keep class com.sun.mail.** { *; }
-keep class javax.mail.** { *; }
-keep class javax.activation.** { *; }
-keep class com.sun.activation.** { *; }
-keep class myjava.** { *; }
-keep class org.apache.harmony.** { *; }

# Riferimenti a classi desktop/Java SE assenti su Android
-dontwarn java.awt.**
-dontwarn java.beans.**
-dontwarn javax.security.sasl.**
-dontwarn org.ietf.jgss.**
-dontwarn sun.security.util.HostnameChecker