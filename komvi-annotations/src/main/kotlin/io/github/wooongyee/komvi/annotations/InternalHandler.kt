package io.github.wooongyee.komvi.annotations

/**
 * Marks a function as a handler for @Internal intents.
 *
 * Functions annotated with @InternalHandler must process intents that are
 * marked with @Internal annotation. The processor will validate this at compile time.
 *
 * @param debug Enable debug logging for this intent handler.
 *              When enabled, logs "Intent received" and "Intent completed" with execution time.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class InternalHandler(
    val debug: Boolean = false
)
