# ProGuard / R8 rules for TG WS Proxy

# Android Components
-keepclassmembers class * extends android.app.Service {
    <init>();
}
-keepclassmembers class * extends android.content.BroadcastReceiver {
    <init>();
}

# JNA ignore desktop java.awt warnings on Android
-dontwarn java.awt.**
-dontwarn com.sun.jna.**

# Keep JNA core classes, interfaces, and native field names (peer, etc.)
-keep class com.sun.jna.** { *; }
-keepclassmembers class com.sun.jna.** { *; }
-keep class * implements com.sun.jna.Library { *; }
-keepclassmembers class * implements com.sun.jna.Library { *; }
-keep class * implements com.sun.jna.Structure { *; }
-keepclassmembers class * implements com.sun.jna.Structure { *; }
-keep class * implements com.sun.jna.Callback { *; }
-keepclassmembers class * implements com.sun.jna.Callback { *; }

# Keep NativeProxy & ProxyLibrary JNA interface
-keep class com.tgws.proxy.ProxyLibrary { *; }
-keepclassmembers class com.tgws.proxy.ProxyLibrary { *; }
-keep class com.tgws.proxy.NativeProxy { *; }
-keepclassmembers class com.tgws.proxy.NativeProxy { *; }

# Keep all native methods across the project
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep Kotlin reflection / coroutines metadata if needed
-keepclassmembers class * extends java.lang.Enum {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
