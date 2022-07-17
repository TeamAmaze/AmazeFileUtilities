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
-keep class com.shockwave.**
-keepclassmembers class * extends com.stephentuso.welcome.WelcomeActivity {
    public static java.lang.String welcomeKey();
}
-keep class ch.qos.** { *; }
-keep class org.slf4j.** { *; }
-keepattributes *Annotation*
-dontwarn ch.qos.logback.core.net.*
-keep class com.masoudss.lib.** { *; }
-keep class linc.com.amplituda.** { *; }
-keep class com.amaze.fileutilities.cast.CastOptionsProvider { *; }
-keep class android.support.** { *; }
-keep class com.google.** { *; }
-keep class com.folioreader.** { *; }
-keep class org.opencv.** { *; }

-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
 <init>(...);
}
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}
-keep class com.bumptech.glide.load.data.ParcelFileDescriptorRewinder$InternalRewinder {
  *** rewind();
}

# Uncomment for DexGuard only
#-keepresourcexmlelements manifest/application/meta-data@value=GlideModule

# https://github.com/mikepenz/AboutLibraries/issues/331
-keepclasseswithmembers class **.R$* {
    public static final int define_*;
}