package io.github.wooongyee.komvi.processor

/**
 * Fully qualified class names used by Komvi annotation processor.
 */
internal object ClassNames {
    const val MVI_VIEW_MODEL = "io.github.wooongyee.komvi.android.MviViewModel"
    const val MVI_VIEW_MODEL_MARKER = "io.github.wooongyee.komvi.core.MviViewModelMarker"
    const val INTENT_SCOPE = "io.github.wooongyee.komvi.core.IntentScope"
    const val VIEW_STATE = "io.github.wooongyee.komvi.core.ViewState"
    const val INTENT = "io.github.wooongyee.komvi.core.Intent"
    const val SIDE_EFFECT = "io.github.wooongyee.komvi.core.SideEffect"

    const val VIEW_ACTION_HANDLER = "io.github.wooongyee.komvi.annotations.ViewActionHandler"
    const val INTERNAL_HANDLER = "io.github.wooongyee.komvi.annotations.InternalHandler"
}
