package io.github.wooongyee.komvi.annotations

/**
 * Marks a function as a handler for @Internal intents.
 *
 * Functions annotated with @InternalHandler must process intents that are
 * marked with @Internal annotation. The processor will validate this at compile time.
 *
 * @param log Enable logging for this intent handler
 * @param track Enable tracking/analytics for this intent handler
 * @param measurePerformance Enable performance measurement for this intent handler
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class InternalHandler(
    val log: Boolean = false,
    val track: Boolean = false,
    val measurePerformance: Boolean = false
)
