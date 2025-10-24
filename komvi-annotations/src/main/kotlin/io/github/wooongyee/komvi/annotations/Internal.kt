package io.github.wooongyee.komvi.annotations

/**
 * Marks an Intent as internal, preventing it from being dispatched from the View layer.
 *
 * Intents annotated with @Internal can only be called from within the ViewModel.
 * Attempting to dispatch these from the View will result in a runtime error.
 */
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.CONSTRUCTOR
)
@Retention(AnnotationRetention.SOURCE)
annotation class Internal
