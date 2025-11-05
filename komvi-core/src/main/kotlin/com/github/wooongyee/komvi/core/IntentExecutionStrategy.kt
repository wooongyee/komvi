package com.github.wooongyee.komvi.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Strategy for executing intent handlers with different coroutine execution policies.
 */
interface IntentExecutionStrategy {
    /**
     * Execute the given intent block with this strategy.
     *
     * @param scope CoroutineScope for launching
     * @param key Unique key for this handler (for tracking jobs)
     * @param block Intent handler block to execute
     */
    fun execute(
        scope: CoroutineScope,
        key: String,
        block: suspend () -> Unit
    )
}

/**
 * Built-in intent execution strategies.
 */
object IntentExecutionStrategies {

    /**
     * Cancels previous execution when new intent arrives.
     * Use case: Search, filter changes, infinite scroll
     */
    class CancelPrevious : IntentExecutionStrategy {
        private val jobs = ConcurrentHashMap<String, Job>()

        override fun execute(
            scope: CoroutineScope,
            key: String,
            block: suspend () -> Unit
        ) {
            jobs[key]?.cancel()
            jobs[key] = scope.launch {
                try {
                    block()
                } finally {
                    jobs.remove(key)
                }
            }
        }
    }

    /**
     * Drops new intent if previous is still running.
     * Use case: Prevent duplicate clicks, network request deduplication
     */
    class Drop : IntentExecutionStrategy {
        private val jobs = ConcurrentHashMap<String, Job>()

        override fun execute(
            scope: CoroutineScope,
            key: String,
            block: suspend () -> Unit
        ) {
            if (jobs[key]?.isActive == true) {
                return // Drop this intent
            }
            jobs[key] = scope.launch {
                try {
                    block()
                } finally {
                    jobs.remove(key)
                }
            }
        }
    }

    /**
     * Queues intents for sequential processing.
     * Use case: Payment, logging, order-sensitive operations
     */
    class Queue : IntentExecutionStrategy {
        private val channels = ConcurrentHashMap<String, Channel<suspend () -> Unit>>()

        override fun execute(
            scope: CoroutineScope,
            key: String,
            block: suspend () -> Unit
        ) {
            val channel = channels.getOrPut(key) {
                Channel<suspend () -> Unit>(Channel.UNLIMITED).also { ch ->
                    scope.launch {
                        for (item in ch) {
                            item()
                        }
                    }
                }
            }
            scope.launch { channel.send(block) }
        }
    }

    /**
     * Executes all intents in parallel.
     * Use case: Analytics events, independent operations
     */
    class Parallel : IntentExecutionStrategy {
        override fun execute(
            scope: CoroutineScope,
            key: String,
            block: suspend () -> Unit
        ) {
            scope.launch {
                block()
            }
        }
    }
}
