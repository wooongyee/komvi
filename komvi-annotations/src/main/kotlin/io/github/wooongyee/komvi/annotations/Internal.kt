package io.github.wooongyee.komvi.annotations

/**
 * Marks internal implementation details that should not be used by library consumers.
 *
 * This annotation is used to indicate implementation details that are internal
 * to the komvi library and may change without notice.
 */
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.CONSTRUCTOR
)
@Retention(AnnotationRetention.SOURCE)
annotation class Internal
