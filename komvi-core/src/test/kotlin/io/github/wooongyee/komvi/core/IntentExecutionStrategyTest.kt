package io.github.wooongyee.komvi.core

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

        // 첫 번째 실행 (취소될 예정)
        strategy.execute(this, "test") {
            delay(100)
            results.add(1)
        }

        // 50ms 후 두 번째 실행 (첫 번째를 취소하고 실행)
        advanceTimeBy(50)
        strategy.execute(this, "test") {
            delay(100)
            results.add(2)
        }

        // 모든 코루틴 완료 대기
        advanceUntilIdle()

        // 2만 실행되어야 함 (1은 취소됨)
        assertEquals(listOf(2), results)
    }

    @Test
    fun `CANCEL_PREVIOUS should not cancel different keys`() = runTest {
        val strategy = IntentExecutionStrategies.CancelPrevious()
        val results = mutableListOf<String>()

        // 다른 key로 실행
        strategy.execute(this, "key1") {
            delay(100)
            results.add("key1")
        }

        strategy.execute(this, "key2") {
            delay(100)
            results.add("key2")
        }

        advanceUntilIdle()

        // 둘 다 실행되어야 함
        assertEquals(2, results.size)
        assertTrue(results.contains("key1"))
        assertTrue(results.contains("key2"))
    }

    @Test
    fun `DROP should ignore new intent if previous is running`() = runTest {
        val strategy = IntentExecutionStrategies.Drop()
        val results = mutableListOf<Int>()

        // 첫 번째 실행
        strategy.execute(this, "test") {
            delay(100)
            results.add(1)
        }

        // 실행 중에 두 번째 시도 (무시되어야 함)
        advanceTimeBy(50)
        strategy.execute(this, "test") {
            delay(100)
            results.add(2)
        }

        advanceUntilIdle()

        // 1만 실행되어야 함 (2는 DROP됨)
        assertEquals(listOf(1), results)
    }

    @Test
    fun `DROP should execute after previous completes`() = runTest {
        val strategy = IntentExecutionStrategies.Drop()
        val results = mutableListOf<Int>()

        // 첫 번째 실행
        strategy.execute(this, "test") {
            delay(100)
            results.add(1)
        }

        // 첫 번째 완료 후 두 번째 시도 (성공해야 함)
        advanceTimeBy(150)
        strategy.execute(this, "test") {
            delay(100)
            results.add(2)
        }

        advanceUntilIdle()

        // 둘 다 실행되어야 함
        assertEquals(listOf(1, 2), results)
    }

    @Test
    fun `QUEUE should execute intents sequentially`() = runTest {
        val strategy = IntentExecutionStrategies.Queue()
        val results = mutableListOf<Int>()

        // backgroundScope을 사용하여 Channel 코루틴이 백그라운드에서 실행되도록 함
        // 3개를 빠르게 추가
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

        // 충분한 시간 대기
        advanceTimeBy(350)

        // 순서대로 실행되어야 함
        assertEquals(listOf(1, 2, 3), results)
    }

    @Test
    fun `QUEUE should maintain order even with different delays`() = runTest {
        val strategy = IntentExecutionStrategies.Queue()
        val results = mutableListOf<String>()

        // backgroundScope 사용
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

        // 충분한 시간 대기
        advanceTimeBy(400)

        // 실행 시간과 관계없이 순서대로
        assertEquals(listOf("long", "short", "medium"), results)
    }

    @Test
    fun `PARALLEL should execute all intents concurrently`() = runTest {
        val strategy = IntentExecutionStrategies.Parallel()
        val results = mutableListOf<Int>()

        // 3개를 동시에 실행
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

        // 모두 실행되어야 함 (순서는 보장 안됨)
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

        // fast가 먼저 완료됨 (100ms)
        advanceTimeBy(60)
        assertEquals(listOf("fast"), results)

        // slow는 나중에 완료됨 (200ms)
        advanceTimeBy(100)
        assertEquals(listOf("fast", "slow"), results)
    }
}
