# ProGuard rules
-keepclassmembers class * extends android.app.Service {
    <init>();
}
-keepclassmembers class * extends android.content.BroadcastReceiver {
    <init>();
}
