package io.github.wooongyee.komvi.annotations

/**
 * Marks a function as an intent handler that processes business logic.
 *
 * Functions annotated with @IntentHandler are responsible for handling
 * intent processing within the MVI container's intent scope.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class IntentHandler
