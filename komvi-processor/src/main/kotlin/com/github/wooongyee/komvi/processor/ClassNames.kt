package com.github.wooongyee.komvi.processor

/**
 * Fully qualified class names used by Komvi annotation processor.
 */
internal object ClassNames {
    const val MVI_VIEW_MODEL = "com.github.wooongyee.komvi.android.MviViewModel"
    const val MVI_VIEW_MODEL_MARKER = "com.github.wooongyee.komvi.core.MviViewModelMarker"
    const val INTENT_SCOPE = "com.github.wooongyee.komvi.core.IntentScope"
    const val VIEW_STATE = "com.github.wooongyee.komvi.core.ViewState"
    const val INTENT = "com.github.wooongyee.komvi.core.Intent"
    const val SIDE_EFFECT = "com.github.wooongyee.komvi.core.SideEffect"

    const val VIEW_ACTION_HANDLER = "com.github.wooongyee.komvi.annotations.ViewActionHandler"
    const val INTERNAL_HANDLER = "com.github.wooongyee.komvi.annotations.InternalHandler"
}
