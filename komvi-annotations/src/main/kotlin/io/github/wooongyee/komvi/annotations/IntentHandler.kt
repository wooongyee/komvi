package io.github.wooongyee.komvi.annotations

import kotlin.reflect.KClass

/**
 * Marks a function as an intent handler that processes business logic.
 *
 * Functions annotated with @IntentHandler are responsible for handling
 * intent processing within the MVI container's intent scope.
 *
 * @param log Enable logging for this intent handler
 * @param track Enable tracking/analytics for this intent handler
 * @param measurePerformance Enable performance measurement for this intent handler
 * @param from Valid source states for this handler (empty = any state)
 * @param to Target state after this handler executes
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class IntentHandler(
    val log: Boolean = false,
    val track: Boolean = false,
    val measurePerformance: Boolean = false,
    val from: Array<KClass<out Any>> = [],
    val to: KClass<out Any> = Nothing::class
)
