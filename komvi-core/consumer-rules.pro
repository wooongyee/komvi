# Komvi Core - Keep all MVI pattern interfaces and implementations
-keep interface io.github.wooongyee.komvi.core.ViewState
-keep interface io.github.wooongyee.komvi.core.Intent
-keep interface io.github.wooongyee.komvi.core.SideEffect

# Keep all classes implementing MVI interfaces
-keep class * implements io.github.wooongyee.komvi.core.ViewState { *; }
-keep class * implements io.github.wooongyee.komvi.core.Intent { *; }
-keep class * implements io.github.wooongyee.komvi.core.SideEffect { *; }

# Keep MviContainer and related classes
-keep class io.github.wooongyee.komvi.core.MviContainer { *; }
-keep class io.github.wooongyee.komvi.core.MviContainerHost { *; }
-keep class io.github.wooongyee.komvi.core.IntentScope { *; }

# Keep factory functions
-keepclassmembers class io.github.wooongyee.komvi.core.MviContainerKt {
    public static ** container(...);
}
