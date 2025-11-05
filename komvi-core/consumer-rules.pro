# Komvi Core - Keep all MVI pattern interfaces and implementations
-keep interface com.github.wooongyee.komvi.core.ViewState
-keep interface com.github.wooongyee.komvi.core.Intent
-keep interface com.github.wooongyee.komvi.core.SideEffect

# Keep all classes implementing MVI interfaces
-keep class * implements com.github.wooongyee.komvi.core.ViewState { *; }
-keep class * implements com.github.wooongyee.komvi.core.Intent { *; }
-keep class * implements com.github.wooongyee.komvi.core.SideEffect { *; }

# Keep MviContainer and related classes
-keep class com.github.wooongyee.komvi.core.MviContainer { *; }
-keep class com.github.wooongyee.komvi.core.MviContainerHost { *; }
-keep class com.github.wooongyee.komvi.core.IntentScope { *; }

# Keep factory functions
-keepclassmembers class com.github.wooongyee.komvi.core.MviContainerKt {
    public static ** container(...);
}
