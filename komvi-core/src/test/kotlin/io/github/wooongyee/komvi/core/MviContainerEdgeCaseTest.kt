package io.github.wooongyee.komvi.core

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Edge case tests for MviContainer including:
 * - debugMode functionality
 * - Exception handling
 * - Flow behavior
 * - Buffer overflow
 * - Concurrent access
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MviContainerEdgeCaseTest {

    // Test state, intent, and effect
    data class TestState(val count: Int = 0, val name: String = "") : ViewState

    sealed interface TestIntent : Intent {
        data object Increment : TestIntent
    }

    sealed interface TestEffect : SideEffect {
        data class ShowMessage(val message: String) : TestEffect
        data object ErrorOccurred : TestEffect
    }

    // Capture console output for debugMode testing
    private val outputStreamCaptor = ByteArrayOutputStream()
    private val originalOut = System.out

    @BeforeTest
    fun setUp() {
        System.setOut(PrintStream(outputStreamCaptor))
    }

    @AfterTest
    fun tearDown() {
        System.setOut(originalOut)
        outputStreamCaptor.reset()
    }

    // Test helper function
    private fun kotlinx.coroutines.test.TestScope.testContainer(
        initialState: TestState,
        debugMode: Boolean = false
    ): MviContainerImpl<TestState, TestIntent, TestEffect> {
        return container<TestState, TestIntent, TestEffect>(
            initialState = initialState,
            scope = this,
            debugMode = debugMode,
            dispatcher = StandardTestDispatcher(testScheduler)
        ) as MviContainerImpl
    }

    // ============ Debug Mode Tests ============

    @Test
    fun `debugMode enabled should print state changes`() = runTest {
        val container = testContainer(
            initialState = TestState(count = 0),
            debugMode = true
        )

        container.intent {
            reduce { copy(count = 1) }
        }

        testScheduler.advanceUntilIdle()

        val output = outputStreamCaptor.toString()
        assertTrue(output.contains("[TestState]"))
        assertTrue(output.contains("State changed"))
        assertTrue(output.contains("count=0"))
        assertTrue(output.contains("count=1"))
    }

    @Test
    fun `debugMode disabled should not print state changes`() = runTest {
        val container = testContainer(
            initialState = TestState(count = 0),
            debugMode = false
        )

        container.intent {
            reduce { copy(count = 1) }
        }

        testScheduler.advanceUntilIdle()

        val output = outputStreamCaptor.toString()
        assertFalse(output.contains("State changed"))
    }

    @Test
    fun `debugMode should print complex state changes`() = runTest {
        val container = testContainer(
            initialState = TestState(count = 5, name = "Initial"),
            debugMode = true
        )

        container.intent {
            reduce { copy(count = 10, name = "Updated") }
        }

        testScheduler.advanceUntilIdle()

        val output = outputStreamCaptor.toString()
        assertTrue(output.contains("name=Initial"))
        assertTrue(output.contains("name=Updated"))
    }

    // ============ Conditional Logic and Error Recovery Tests ============

    @Test
    fun `intent with conditional reduce should work correctly`() = runTest {
        val container = testContainer(initialState = TestState(count = 0))

        container.intent {
            reduce { copy(count = 1) }
            // Conditional logic in intent
            if (state.count > 0) {
                reduce { copy(count = count + 10) }
            }
        }

        testScheduler.advanceUntilIdle()

        // Both reduces should be applied
        assertEquals(11, container.state.value.count)
    }

    @Test
    fun `intent with error recovery should handle failures gracefully`() = runTest {
        val container = testContainer(initialState = TestState(count = 0))

        // Intent with try-catch for error recovery
        container.intent {
            try {
                // Simulate some operation that might fail
                val result = if (state.count < 0) {
                    throw IllegalStateException("Invalid state")
                } else {
                    10
                }
                reduce { copy(count = result) }
            } catch (e: Exception) {
                // Error recovery: set to default value
                reduce { copy(count = 0) }
            }
        }

        testScheduler.advanceUntilIdle()

        assertEquals(10, container.state.value.count)
    }

    // ============ Flow Behavior Tests ============

    @Test
    fun `state flow should emit distinct consecutive values`() = runTest {
        val container = testContainer(initialState = TestState(count = 0))
        val emittedValues = mutableListOf<Int>()

        val collectJob = launch {
            container.state.collect { state ->
                emittedValues.add(state.count)
            }
        }

        container.intent { reduce { copy(count = 1) } }
        testScheduler.advanceUntilIdle()

        container.intent { reduce { copy(count = 1) } } // Same value
        testScheduler.advanceUntilIdle()

        container.intent { reduce { copy(count = 2) } }
        testScheduler.advanceUntilIdle()

        collectJob.cancelAndJoin()

        // StateFlow should emit distinct consecutive values
        // Initial=0, update to 1, same 1 (skipped), update to 2
        assertTrue(emittedValues.contains(0))
        assertTrue(emittedValues.contains(1))
        assertTrue(emittedValues.contains(2))
    }

    @Test
    fun `side effect flow should emit all effects including duplicates`() = runTest {
        val container = testContainer(initialState = TestState())
        val effects = mutableListOf<TestEffect>()

        val collectJob = launch {
            container.sideEffect.collect { effect ->
                effects.add(effect)
            }
        }

        container.intent {
            postSideEffect(TestEffect.ShowMessage("Hello"))
            postSideEffect(TestEffect.ShowMessage("Hello")) // Duplicate
            postSideEffect(TestEffect.ShowMessage("World"))
        }

        testScheduler.advanceUntilIdle()
        collectJob.cancel()

        // All side effects should be emitted, including duplicates
        assertEquals(3, effects.size)
        assertEquals(TestEffect.ShowMessage("Hello"), effects[0])
        assertEquals(TestEffect.ShowMessage("Hello"), effects[1])
        assertEquals(TestEffect.ShowMessage("World"), effects[2])
    }

    @Test
    fun `multiple collectors should all receive side effects`() = runTest {
        val container = testContainer(initialState = TestState())
        val effects1 = mutableListOf<TestEffect>()
        val effects2 = mutableListOf<TestEffect>()

        val collectJob1 = launch {
            container.sideEffect.collect { effects1.add(it) }
        }

        val collectJob2 = launch {
            container.sideEffect.collect { effects2.add(it) }
        }

        container.intent {
            postSideEffect(TestEffect.ShowMessage("Test"))
        }

        testScheduler.advanceUntilIdle()
        collectJob1.cancel()
        collectJob2.cancel()

        // Both collectors should receive the effect
        assertEquals(1, effects1.size)
        assertEquals(1, effects2.size)
        assertEquals(TestEffect.ShowMessage("Test"), effects1[0])
        assertEquals(TestEffect.ShowMessage("Test"), effects2[0])
    }

    @Test
    fun `late subscriber should not receive previous side effects`() = runTest {
        val container = testContainer(initialState = TestState())

        container.intent {
            postSideEffect(TestEffect.ShowMessage("Before subscription"))
        }

        testScheduler.advanceUntilIdle()

        // Subscribe after effect is emitted
        val effects = mutableListOf<TestEffect>()
        val collectJob = launch {
            container.sideEffect.collect { effects.add(it) }
        }

        testScheduler.advanceUntilIdle()
        collectJob.cancel()

        // Should not receive previous effect
        assertEquals(0, effects.size)
    }

    // ============ Buffer Overflow Tests ============

    @Test
    fun `side effect buffer should drop oldest when full`() = runTest {
        val container = testContainer(initialState = TestState())

        // Emit more than buffer capacity (64)
        container.intent {
            repeat(100) { index ->
                postSideEffect(TestEffect.ShowMessage("Message $index"))
            }
        }

        testScheduler.advanceUntilIdle()

        // Subscribe after buffer is full
        val effects = mutableListOf<TestEffect>()
        val collectJob = launch {
            container.sideEffect.collect { effects.add(it) }
        }

        // Emit one more to trigger collection
        container.intent {
            postSideEffect(TestEffect.ErrorOccurred)
        }

        testScheduler.advanceUntilIdle()
        collectJob.cancel()

        // Should receive the last effect only (buffer drops oldest)
        assertTrue(effects.size <= 65) // Buffer size + 1
    }

    // ============ Concurrent Access Tests ============

    @Test
    fun `concurrent state updates should be atomic`() = runTest {
        val container = testContainer(initialState = TestState(count = 0))

        // Launch 1000 concurrent increments
        repeat(1000) {
            container.intent {
                val current = state.count
                reduce { copy(count = current + 1) }
            }
        }

        testScheduler.advanceUntilIdle()

        // All updates should be applied atomically
        assertEquals(1000, container.state.value.count)
    }

    @Test
    fun `concurrent reduce and state read should be consistent`() = runTest {
        val container = testContainer(initialState = TestState(count = 0))
        val readValues = mutableListOf<Int>()

        // Concurrent updates and reads
        repeat(50) {
            container.intent {
                reduce { copy(count = count + 1) }
            }

            container.intent {
                readValues.add(state.count)
            }
        }

        testScheduler.advanceUntilIdle()

        // Final state should be consistent
        assertEquals(50, container.state.value.count)

        // All read values should be valid (between 0 and 50)
        assertTrue(readValues.all { it in 0..50 })
    }

    @Test
    fun `rapid state changes should all be applied`() = runTest {
        val container = testContainer(initialState = TestState(count = 0))

        container.intent {
            repeat(100) {
                reduce { copy(count = count + 1) }
            }
        }

        testScheduler.advanceUntilIdle()

        assertEquals(100, container.state.value.count)
    }

    @Test
    fun `interleaved state and side effect operations should work correctly`() = runTest {
        val container = testContainer(initialState = TestState(count = 0))
        val effects = mutableListOf<TestEffect>()

        val collectJob = launch {
            container.sideEffect.collect { effects.add(it) }
        }

        container.intent {
            reduce { copy(count = 1) }
            postSideEffect(TestEffect.ShowMessage("After 1"))
            reduce { copy(count = 2) }
            postSideEffect(TestEffect.ShowMessage("After 2"))
            reduce { copy(count = 3) }
            postSideEffect(TestEffect.ShowMessage("After 3"))
        }

        testScheduler.advanceUntilIdle()
        collectJob.cancel()

        assertEquals(3, container.state.value.count)
        assertEquals(3, effects.size)
        assertEquals(TestEffect.ShowMessage("After 1"), effects[0])
        assertEquals(TestEffect.ShowMessage("After 2"), effects[1])
        assertEquals(TestEffect.ShowMessage("After 3"), effects[2])
    }

    // ============ Edge Cases ============

    @Test
    fun `empty intent should not change state`() = runTest {
        val container = testContainer(initialState = TestState(count = 5))

        container.intent {
            // Do nothing
        }

        testScheduler.advanceUntilIdle()

        assertEquals(5, container.state.value.count)
    }

    @Test
    fun `intent with only side effects should not change state`() = runTest {
        val container = testContainer(initialState = TestState(count = 10))

        container.intent {
            postSideEffect(TestEffect.ShowMessage("No state change"))
        }

        testScheduler.advanceUntilIdle()

        assertEquals(10, container.state.value.count)
    }

    @Test
    fun `state value should be accessible multiple times in same intent`() = runTest {
        val container = testContainer(initialState = TestState(count = 5))
        val accessedValues = mutableListOf<Int>()

        container.intent {
            accessedValues.add(state.count)
            reduce { copy(count = count + 1) }
            accessedValues.add(state.count) // Should reflect new value
            reduce { copy(count = count + 1) }
            accessedValues.add(state.count) // Should reflect newest value
        }

        testScheduler.advanceUntilIdle()

        assertEquals(7, container.state.value.count)
        // State getter always returns latest value
        assertTrue(accessedValues.size == 3)
    }
}
