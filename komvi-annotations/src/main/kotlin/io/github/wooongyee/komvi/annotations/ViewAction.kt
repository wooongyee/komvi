package io.github.wooongyee.komvi.annotations

/**
 * Marks a function as a view action that processes user interactions.
 *
 * Functions annotated with @ViewAction are intended to handle UI events
 * and trigger corresponding intents in the MVI container.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class ViewAction
