# Komvi Compose - Keep Compose extension functions
-keepclassmembers class io.github.wooongyee.komvi.compose.SideEffectExtKt {
    public static ** collectSideEffect(...);
}

-keepclassmembers class io.github.wooongyee.komvi.compose.StateExtKt {
    public static ** collectAsStateWithLifecycle(...);
}
