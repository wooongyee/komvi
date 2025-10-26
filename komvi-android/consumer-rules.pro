# Komvi Android - Keep MviViewModel and extensions
-keep class io.github.wooongyee.komvi.android.MviViewModel { *; }

# Keep all classes extending MviViewModel
-keep class * extends io.github.wooongyee.komvi.android.MviViewModel {
    <init>(...);
    public protected *;
}

# Keep ViewModel extension functions
-keepclassmembers class io.github.wooongyee.komvi.android.ViewModelExtKt {
    public static ** container(...);
}
