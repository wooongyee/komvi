package io.github.wooongyee.komvi.annotations

/**
 * Marks a function as a handler for @ViewAction intents.
 *
 * Functions annotated with @ViewActionHandler must process intents that are
 * marked with @ViewAction annotation. The processor will validate this at compile time.
 *
 * @param log Enable logging for this intent handler.
 *            When enabled and ViewModel's debugMode is true, logs "Intent received: [intent]".
 * @param executionMode Execution mode for this handler (coroutine execution strategy).
 *                      핸들러의 실행 모드 (코루틴 실행 전략).
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class ViewActionHandler(
    val log: Boolean = true,
    val executionMode: ExecutionMode = ExecutionMode.PARALLEL
)
