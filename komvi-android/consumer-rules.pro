# Komvi Android - Keep MviViewModel
-keep class io.github.wooongyee.komvi.android.MviViewModel { *; }

# Keep all classes extending MviViewModel
-keep class * extends io.github.wooongyee.komvi.android.MviViewModel {
    <init>(...);
    public protected *;
}
