# Manifest-declared components are kept by the default rules, but the capture
# service is bound by the system by name, so keep it explicit and unrenamed.
-keep class com.tide.app.notifications.NotificationCaptureService { *; }

# Room generates implementations that are looked up reflectively at runtime.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep @androidx.room.Entity class * { *; }

# Enum valueOf is used by the Room type converters for DeliveryMode/RuleSource.
-keepclassmembers enum com.tide.app.data.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
