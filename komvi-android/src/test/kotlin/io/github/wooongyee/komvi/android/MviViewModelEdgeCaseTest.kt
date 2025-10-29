package io.github.wooongyee.komvi.android

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import io.github.wooongyee.komvi.annotations.InternalKomviApi
import io.github.wooongyee.komvi.core.Intent
import io.github.wooongyee.komvi.core.SideEffect
import io.github.wooongyee.komvi.core.ViewState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.parcelize.Parcelize
import org.junit.After
import org.junit.Before
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Edge case tests for MviViewModel including:
 * - ViewModel lifecycle scenarios
 * - SavedStateHandle edge cases
 * - Coroutine scope cancellation
 * - Custom dispatcher behavior
 */
@OptIn(InternalKomviApi::class, ExperimentalCoroutinesApi::class)
class MviViewModelEdgeCaseTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // Test state, intent, and effect
    @Parcelize
    data class TestState(
        val count: Int = 0,
        val name: String = "",
        val isLoading: Boolean = false
    ) : ViewState, Parcelable

    sealed interface TestIntent : Intent {
        data object Increment : TestIntent
    }

    sealed interface TestEffect : SideEffect {
        data class ShowMessage(val message: String) : TestEffect
        data object Complete : TestEffect
    }

    // Test ViewModel implementation
    inner class TestViewModel(
        initialState: TestState = TestState(),
        savedStateHandle: SavedStateHandle? = null,
        stateKey: String = "mvi_state",
        debugMode: Boolean = false
    ) : MviViewModel<TestState, TestIntent, TestEffect>(
        initialState = initialState,
        savedStateHandle = savedStateHandle,
        stateKey = stateKey,
        debugMode = debugMode,
        dispatcher = testDispatcher
    ) {
        fun increment() = executeHandler {
            reduce { copy(count = count + 1) }
        }

        fun updateName(newName: String) = executeHandler {
            reduce { copy(name = newName) }
        }

        fun setLoading(loading: Boolean) = executeHandler {
            reduce { copy(isLoading = loading) }
        }

        fun emitEffect(message: String) = executeHandler {
            postSideEffect(TestEffect.ShowMessage(message))
        }

        fun performLongOperation() = executeHandler {
            reduce { copy(isLoading = true) }
            // Simulate long operation
            delay(1000)
            reduce { copy(isLoading = false, count = count + 1) }
            postSideEffect(TestEffect.Complete)
        }

        fun multipleStateChanges() = executeHandler {
            reduce { copy(count = 1) }
            reduce { copy(name = "First") }
            reduce { copy(isLoading = true) }
            reduce { copy(count = 2, name = "Second", isLoading = false) }
        }
    }

    // ============ ViewModel Lifecycle Tests ============

    @Test
    fun `viewModelScope should be used for intent execution`() = runTest(testDispatcher) {
        val viewModel = TestViewModel()

        viewModel.increment()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.state.value.count)

        // ViewModelScope is still active, so operations should work
        viewModel.increment()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, viewModel.state.value.count)
    }

    @Test
    fun `long running operations should work correctly`() = runTest(testDispatcher) {
        val viewModel = TestViewModel()

        // Start long operation
        viewModel.performLongOperation()

        // Should be in loading state
        testDispatcher.scheduler.advanceTimeBy(500)
        assertEquals(true, viewModel.state.value.isLoading)

        // Complete the operation
        testDispatcher.scheduler.advanceTimeBy(1000)

        // Should be done
        assertEquals(false, viewModel.state.value.isLoading)
        assertEquals(1, viewModel.state.value.count)
    }

    // ============ SavedStateHandle Edge Cases ============

    @Test
    fun `SavedStateHandle with custom stateKey should work`() = runTest(testDispatcher) {
        val savedStateHandle = SavedStateHandle()
        val customKey = "custom_key"

        val viewModel1 = TestViewModel(
            initialState = TestState(count = 0),
            savedStateHandle = savedStateHandle,
            stateKey = customKey
        )
        viewModel1.increment()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(TestState(count = 1), savedStateHandle.get<TestState>(customKey))
        assertEquals(null, savedStateHandle.get<TestState>("mvi_state")) // Default key should be empty

        // Restore with same custom key
        val viewModel2 = TestViewModel(
            initialState = TestState(count = 0),
            savedStateHandle = savedStateHandle,
            stateKey = customKey
        )

        assertEquals(1, viewModel2.state.value.count)
    }

    @Test
    fun `SavedStateHandle should save complex state objects`() = runTest(testDispatcher) {
        val savedStateHandle = SavedStateHandle()

        val viewModel1 = TestViewModel(
            initialState = TestState(count = 0),
            savedStateHandle = savedStateHandle
        )
        viewModel1.increment()
        viewModel1.updateName("Test")
        viewModel1.setLoading(true)
        testDispatcher.scheduler.advanceUntilIdle()

        val savedState = savedStateHandle.get<TestState>("mvi_state")
        assertEquals(1, savedState?.count)
        assertEquals("Test", savedState?.name)
        assertEquals(true, savedState?.isLoading)

        // Restore
        val viewModel2 = TestViewModel(
            initialState = TestState(),
            savedStateHandle = savedStateHandle
        )

        assertEquals(1, viewModel2.state.value.count)
        assertEquals("Test", viewModel2.state.value.name)
        assertEquals(true, viewModel2.state.value.isLoading)
    }

    @Test
    fun `SavedStateHandle should update on every state change`() = runTest(testDispatcher) {
        val savedStateHandle = SavedStateHandle()
        val viewModel = TestViewModel(
            initialState = TestState(count = 0),
            savedStateHandle = savedStateHandle
        )

        // Multiple updates
        viewModel.increment()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(TestState(count = 1), savedStateHandle.get<TestState>("mvi_state"))

        viewModel.increment()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(TestState(count = 2), savedStateHandle.get<TestState>("mvi_state"))

        viewModel.updateName("Updated")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(TestState(count = 2, name = "Updated"), savedStateHandle.get<TestState>("mvi_state"))
    }

    @Test
    fun `SavedStateHandle without initial saved state should use provided initialState`() = runTest(testDispatcher) {
        val savedStateHandle = SavedStateHandle() // Empty SavedStateHandle

        val viewModel = TestViewModel(
            initialState = TestState(count = 42, name = "Initial"),
            savedStateHandle = savedStateHandle
        )

        // Should use initialState since SavedStateHandle is empty
        assertEquals(42, viewModel.state.value.count)
        assertEquals("Initial", viewModel.state.value.name)
    }

    @Test
    fun `SavedStateHandle with existing state should override initialState`() = runTest(testDispatcher) {
        val savedStateHandle = SavedStateHandle()
        savedStateHandle["mvi_state"] = TestState(count = 100, name = "Saved")

        val viewModel = TestViewModel(
            initialState = TestState(count = 0, name = "Initial"),
            savedStateHandle = savedStateHandle
        )

        // Should use saved state, not initialState
        assertEquals(100, viewModel.state.value.count)
        assertEquals("Saved", viewModel.state.value.name)
    }

    // ============ Dispatcher and Coroutine Tests ============

    @Test
    fun `custom dispatcher should be used for intent execution`() = runTest(testDispatcher) {
        val viewModel = TestViewModel()

        viewModel.increment()

        // Nothing should happen yet (dispatcher hasn't advanced)
        assertEquals(0, viewModel.state.value.count)

        // Advance the test dispatcher
        testDispatcher.scheduler.advanceUntilIdle()

        // Now the update should be applied
        assertEquals(1, viewModel.state.value.count)
    }

    @Test
    fun `multiple state changes in single intent should all be applied`() = runTest(testDispatcher) {
        val viewModel = TestViewModel()

        viewModel.multipleStateChanges()
        testDispatcher.scheduler.advanceUntilIdle()

        // Final state should reflect all changes
        assertEquals(2, viewModel.state.value.count)
        assertEquals("Second", viewModel.state.value.name)
        assertEquals(false, viewModel.state.value.isLoading)
    }

    @Test
    fun `side effects should be emitted even if viewModel state doesn't change`() = runTest(testDispatcher) {
        val viewModel = TestViewModel()
        val effects = mutableListOf<TestEffect>()

        val collectJob = launch {
            viewModel.sideEffect.collect { effects.add(it) }
        }

        viewModel.emitEffect("Test 1")
        viewModel.emitEffect("Test 2")
        viewModel.emitEffect("Test 3")
        testDispatcher.scheduler.advanceUntilIdle()

        collectJob.cancel()

        assertEquals(3, effects.size)
        assertTrue(effects[0] is TestEffect.ShowMessage)
        assertTrue(effects[1] is TestEffect.ShowMessage)
        assertTrue(effects[2] is TestEffect.ShowMessage)
    }

    // ============ State Flow Behavior Tests ============

    @Test
    fun `state should be hot flow - emits current value immediately`() = runTest(testDispatcher) {
        val viewModel = TestViewModel(initialState = TestState(count = 5))

        // Collect state after viewModel is created
        val states = mutableListOf<Int>()
        val collectJob = launch {
            viewModel.state.collect { states.add(it.count) }
        }

        testDispatcher.scheduler.advanceUntilIdle()

        // Should immediately receive current state
        assertTrue(states.contains(5))

        collectJob.cancel()
    }

    @Test
    fun `multiple collectors should all receive state updates`() = runTest(testDispatcher) {
        val viewModel = TestViewModel()
        val states1 = mutableListOf<Int>()
        val states2 = mutableListOf<Int>()

        val collectJob1 = launch {
            viewModel.state.collect { states1.add(it.count) }
        }

        val collectJob2 = launch {
            viewModel.state.collect { states2.add(it.count) }
        }

        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.increment()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.increment()
        testDispatcher.scheduler.advanceUntilIdle()

        collectJob1.cancel()
        collectJob2.cancel()

        // Both collectors should receive all updates
        assertTrue(states1.contains(0)) // Initial
        assertTrue(states1.contains(1))
        assertTrue(states1.contains(2))

        assertTrue(states2.contains(0)) // Initial
        assertTrue(states2.contains(1))
        assertTrue(states2.contains(2))
    }

    // ============ Debug Mode Tests ============

    @Test
    fun `debugMode enabled should not affect functionality`() = runTest(testDispatcher) {
        val viewModel1 = TestViewModel(debugMode = true)
        val viewModel2 = TestViewModel(debugMode = false)

        viewModel1.increment()
        viewModel1.updateName("Test")
        viewModel2.increment()
        viewModel2.updateName("Test")

        testDispatcher.scheduler.advanceUntilIdle()

        // Both should have same final state
        assertEquals(viewModel2.state.value, viewModel1.state.value)
    }

    // ============ Integration Tests ============

    @Test
    fun `viewModel should handle realistic user flow`() = runTest(testDispatcher) {
        val savedStateHandle = SavedStateHandle()
        val viewModel = TestViewModel(
            initialState = TestState(),
            savedStateHandle = savedStateHandle
        )
        val effects = mutableListOf<TestEffect>()

        val collectJob = launch {
            viewModel.sideEffect.collect { effects.add(it) }
        }

        // Simulate user flow
        viewModel.updateName("User1")
        viewModel.increment()
        viewModel.setLoading(true)
        viewModel.emitEffect("Loading started")
        viewModel.increment()
        viewModel.setLoading(false)
        viewModel.emitEffect("Loading completed")

        testDispatcher.scheduler.advanceUntilIdle()
        collectJob.cancel()

        // Verify final state
        assertEquals(2, viewModel.state.value.count)
        assertEquals("User1", viewModel.state.value.name)
        assertEquals(false, viewModel.state.value.isLoading)

        // Verify effects
        assertEquals(2, effects.size)

        // Verify state was saved
        val savedState = savedStateHandle.get<TestState>("mvi_state")
        assertEquals(2, savedState?.count)
        assertEquals("User1", savedState?.name)
    }
}
