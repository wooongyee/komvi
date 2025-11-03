package io.github.wooongyee.komvi.annotations

/**
 * Marks a function as a handler for @ViewAction intents.
 *
 * Functions annotated with @ViewActionHandler must process intents that are
 * marked with @ViewAction annotation. The processor will validate this at compile time.
 *
 * @param debug Enable debug logging for this intent handler.
 *              When enabled, logs "Intent received" and "Intent completed" with execution time.
 * @param executionMode Execution mode for this handler (coroutine execution strategy).
 *                      핸들러의 실행 모드 (코루틴 실행 전략).
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class ViewActionHandler(
    val debug: Boolean = true,
    val executionMode: ExecutionMode = ExecutionMode.PARALLEL
)
