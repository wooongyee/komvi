# Komvi Android - Keep MviViewModel
-keep class com.github.wooongyee.komvi.android.MviViewModel { *; }

# Keep all classes extending MviViewModel
-keep class * extends com.github.wooongyee.komvi.android.MviViewModel {
    <init>(...);
    public protected *;
}
