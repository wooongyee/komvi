package com.github.wooongyee.komvi.core

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class IntentExecutionStrategyTest {

    @Test
    fun `CANCEL_PREVIOUS should cancel previous job when new one arrives`() = runTest {
        val strategy = IntentExecutionStrategies.CancelPrevious()
        val results = mutableListOf<Int>()

        // First execution (will be cancelled)
        strategy.execute(this, "test") {
            delay(100)
            results.add(1)
        }

        // Second execution after 50ms (cancels first and runs)
        advanceTimeBy(50)
        strategy.execute(this, "test") {
            delay(100)
            results.add(2)
        }

        // Wait for all coroutines to complete
        advanceUntilIdle()

        // Only 2 should execute (1 is cancelled)
        assertEquals(listOf(2), results)
    }

    @Test
    fun `CANCEL_PREVIOUS should not cancel different keys`() = runTest {
        val strategy = IntentExecutionStrategies.CancelPrevious()
        val results = mutableListOf<String>()

        // Execute with different keys
        strategy.execute(this, "key1") {
            delay(100)
            results.add("key1")
        }

        strategy.execute(this, "key2") {
            delay(100)
            results.add("key2")
        }

        advanceUntilIdle()

        // Both should execute
        assertEquals(2, results.size)
        assertTrue(results.contains("key1"))
        assertTrue(results.contains("key2"))
    }

    @Test
    fun `DROP should ignore new intent if previous is running`() = runTest {
        val strategy = IntentExecutionStrategies.Drop()
        val results = mutableListOf<Int>()

        // First execution
        strategy.execute(this, "test") {
            delay(100)
            results.add(1)
        }

        // Second attempt while running (should be dropped)
        advanceTimeBy(50)
        strategy.execute(this, "test") {
            delay(100)
            results.add(2)
        }

        advanceUntilIdle()

        // Only 1 should execute (2 is dropped)
        assertEquals(listOf(1), results)
    }

    @Test
    fun `DROP should execute after previous completes`() = runTest {
        val strategy = IntentExecutionStrategies.Drop()
        val results = mutableListOf<Int>()

        // First execution
        strategy.execute(this, "test") {
            delay(100)
            results.add(1)
        }

        // Second attempt after first completes (should succeed)
        advanceTimeBy(150)
        strategy.execute(this, "test") {
            delay(100)
            results.add(2)
        }

        advanceUntilIdle()

        // Both should execute
        assertEquals(listOf(1, 2), results)
    }

    @Test
    fun `QUEUE should execute intents sequentially`() = runTest {
        val strategy = IntentExecutionStrategies.Queue()
        val results = mutableListOf<Int>()

        // Use backgroundScope to run Channel coroutine in background
        // Add 3 items quickly
        strategy.execute(backgroundScope, "test") {
            delay(100)
            results.add(1)
        }

        strategy.execute(backgroundScope, "test") {
            delay(100)
            results.add(2)
        }

        strategy.execute(backgroundScope, "test") {
            delay(100)
            results.add(3)
        }

        // Wait sufficient time
        advanceTimeBy(350)

        // Should execute in order
        assertEquals(listOf(1, 2, 3), results)
    }

    @Test
    fun `QUEUE should maintain order even with different delays`() = runTest {
        val strategy = IntentExecutionStrategies.Queue()
        val results = mutableListOf<String>()

        // Use backgroundScope
        strategy.execute(backgroundScope, "test") {
            delay(200)
            results.add("long")
        }

        strategy.execute(backgroundScope, "test") {
            delay(50)
            results.add("short")
        }

        strategy.execute(backgroundScope, "test") {
            delay(100)
            results.add("medium")
        }

        // Wait sufficient time
        advanceTimeBy(400)

        // In order regardless of execution time
        assertEquals(listOf("long", "short", "medium"), results)
    }

    @Test
    fun `PARALLEL should execute all intents concurrently`() = runTest {
        val strategy = IntentExecutionStrategies.Parallel()
        val results = mutableListOf<Int>()

        // Execute 3 concurrently
        strategy.execute(this, "test") {
            delay(100)
            results.add(1)
        }

        strategy.execute(this, "test") {
            delay(100)
            results.add(2)
        }

        strategy.execute(this, "test") {
            delay(100)
            results.add(3)
        }

        advanceUntilIdle()

        // All should execute (order not guaranteed)
        assertEquals(3, results.size)
        assertTrue(results.contains(1))
        assertTrue(results.contains(2))
        assertTrue(results.contains(3))
    }

    @Test
    fun `PARALLEL should not wait for previous completion`() = runTest {
        val strategy = IntentExecutionStrategies.Parallel()
        val results = mutableListOf<String>()

        strategy.execute(this, "test") {
            delay(200)
            results.add("slow")
        }

        advanceTimeBy(50)

        strategy.execute(this, "test") {
            delay(50)
            results.add("fast")
        }

        // fast completes first (100ms)
        advanceTimeBy(60)
        assertEquals(listOf("fast"), results)

        // slow completes later (200ms)
        advanceTimeBy(100)
        assertEquals(listOf("fast", "slow"), results)
    }
}
