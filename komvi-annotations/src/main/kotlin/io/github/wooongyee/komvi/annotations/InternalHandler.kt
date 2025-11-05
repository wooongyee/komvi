package io.github.wooongyee.komvi.annotations

/**
 * Marks a function as a handler for @Internal intents.
 *
 * Functions annotated with @InternalHandler must process intents that are
 * marked with @Internal annotation. The processor will validate this at compile time.
 *
 * @param log Enable logging for this intent handler.
 *            When enabled and ViewModel's debugMode is true, logs "Intent received: [intent]".
 * @param executionMode Execution mode for this handler (coroutine execution strategy).
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class InternalHandler(
    val log: Boolean = true,
    val executionMode: ExecutionMode = ExecutionMode.PARALLEL
)
