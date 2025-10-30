package io.github.wooongyee.komvi.annotations

/**
 * Defines coroutine execution strategy for intent handler.
 *
 * Intent 핸들러의 코루틴 실행 전략을 정의합니다.
 */
enum class ExecutionMode {
    /**
     * Cancels previous execution when new intent arrives.
     * Use for: Search input, filter changes, infinite scroll
     */
    CANCEL_PREVIOUS,

    /**
     * Drops new intent if previous is still running.
     * Use for: Duplicate click prevention, network request deduplication
     */
    DROP,

    /**
     * Queues intents for sequential processing.
     * Use for: Payment, logging, order-sensitive operations
     */
    QUEUE,

    /**
     * Executes all intents in parallel (default).
     * Use for: Analytics events, independent operations
     */
    PARALLEL
}
