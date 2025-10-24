package io.github.wooongyee.komvi.core

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class MviContainerTest {

    // Test state, intent, and effect
    data class TestState(val count: Int = 0) : ViewState

    sealed interface TestIntent : Intent {
        data object Increment : TestIntent
        data object Decrement : TestIntent
    }

    sealed interface TestEffect : SideEffect {
        data object ShowToast : TestEffect
    }

    @Test
    fun `initial state should be set correctly`() = runTest {
        val container = container<TestState, TestIntent, TestEffect>(
            initialState = TestState(count = 5),
            scope = this
        )

        assertEquals(5, container.state.value.count)
    }

    @Test
    fun `reduce should update state`() = runTest {
        val container = container<TestState, TestIntent, TestEffect>(
            initialState = TestState(count = 0),
            scope = this
        )

        container.intent {
            reduce { copy(count = count + 1) }
        }

        testScheduler.advanceUntilIdle()

        assertEquals(1, container.state.value.count)
    }

    @Test
    fun `multiple reduce calls in single intent should update state sequentially`() = runTest {
        val container = container<TestState, TestIntent, TestEffect>(
            initialState = TestState(count = 0),
            scope = this
        )

        container.intent {
            reduce { copy(count = count + 1) }
            reduce { copy(count = count + 1) }
            reduce { copy(count = count + 1) }
        }

        testScheduler.advanceUntilIdle()

        assertEquals(3, container.state.value.count)
    }

    @Test
    fun `concurrent intent calls should be thread-safe`() = runTest {
        val container = container<TestState, TestIntent, TestEffect>(
            initialState = TestState(count = 0),
            scope = this
        )

        // Launch multiple concurrent intents
        repeat(100) {
            container.intent {
                reduce { copy(count = count + 1) }
            }
        }

        testScheduler.advanceUntilIdle()

        // All 100 increments should be applied
        assertEquals(100, container.state.value.count)
    }

    @Test
    fun `postSideEffect should emit side effect`() = runTest {
        val container = container<TestState, TestIntent, TestEffect>(
            initialState = TestState(),
            scope = this
        )

        // Start collecting before emitting
        val effects = mutableListOf<TestEffect>()
        val collectJob = launch {
            container.sideEffect.collect { effects.add(it) }
        }

        container.intent {
            postSideEffect(TestEffect.ShowToast)
        }

        testScheduler.advanceUntilIdle()
        collectJob.cancel()

        assertEquals(1, effects.size)
        assertEquals(TestEffect.ShowToast, effects.first())
    }

    @Test
    fun `state should be accessible in intent scope`() = runTest {
        val container = container<TestState, TestIntent, TestEffect>(
            initialState = TestState(count = 10),
            scope = this
        )

        container.intent {
            val currentCount = state.count
            reduce { copy(count = currentCount + 5) }
        }

        testScheduler.advanceUntilIdle()

        assertEquals(15, container.state.value.count)
    }

    @Test
    fun `multiple intent calls should be independent`() = runTest {
        val container = container<TestState, TestIntent, TestEffect>(
            initialState = TestState(count = 0),
            scope = this
        )

        container.intent {
            reduce { copy(count = count + 1) }
        }

        container.intent {
            reduce { copy(count = count + 10) }
        }

        testScheduler.advanceUntilIdle()

        assertEquals(11, container.state.value.count)
    }
}
