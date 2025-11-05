# Komvi Processor - Keep generated dispatch functions
-keep class **_Dispatch** { *; }
-keepclassmembers class **_Dispatch** {
    public static ** dispatch(...);
}

# Keep annotation processor related classes
-keep class com.github.wooongyee.komvi.annotations.** { *; }
