package com.github.wooongyee.komvi.android

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.github.wooongyee.komvi.annotations.InternalKomviApi
import com.github.wooongyee.komvi.core.Intent
import com.github.wooongyee.komvi.core.SideEffect
import com.github.wooongyee.komvi.core.ViewState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
import kotlin.test.assertFailsWith

@OptIn(InternalKomviApi::class, ExperimentalCoroutinesApi::class)
class MviViewModelTest {

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
    data class TestState(val count: Int = 0) : ViewState, Parcelable

    sealed interface TestIntent : Intent {
        data object Increment : TestIntent
        data object Decrement : TestIntent
    }

    sealed interface TestEffect : SideEffect {
        data object ShowToast : TestEffect
    }

    // Test ViewModel implementation
    inner class TestViewModel(
        initialState: TestState = TestState(),
        savedStateHandle: SavedStateHandle? = null,
        debugMode: Boolean = false
    ) : MviViewModel<TestState, TestIntent, TestEffect>(
        initialState = initialState,
        savedStateHandle = savedStateHandle,
        debugMode = debugMode
    ) {
        override val dispatcher = testDispatcher

        // Public function that executes handlers
        fun increment() = executeHandler(
            block = {
                reduce { copy(count = count + 1) }
            },
            handlerKey = "TestViewModel.increment"
        )

        fun decrement() = executeHandler(
            block = {
                reduce { copy(count = count - 1) }
            },
            handlerKey = "TestViewModel.decrement"
        )

        fun emitEffect() = executeHandler(
            block = {
                postSideEffect(TestEffect.ShowToast)
            },
            handlerKey = "TestViewModel.emitEffect"
        )

        fun accessState() = executeHandler(
            block = {
                val currentCount = state.count
                reduce { copy(count = currentCount + 10) }
            },
            handlerKey = "TestViewModel.accessState"
        )
    }

    @Test
    fun `initial state should be set correctly`() = runTest(testDispatcher) {
        val viewModel = TestViewModel(initialState = TestState(count = 5))

        assertEquals(5, viewModel.state.value.count)
    }

    @Test
    fun `handler should allow state updates via reduce`() = runTest(testDispatcher) {
        val viewModel = TestViewModel()

        viewModel.increment()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.state.value.count)
    }

    @Test
    fun `multiple handler calls should update state sequentially`() = runTest(testDispatcher) {
        val viewModel = TestViewModel()

        viewModel.increment()
        viewModel.increment()
        viewModel.decrement()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.state.value.count)
    }

    @Test
    fun `handler should allow posting side effects`() = runTest(testDispatcher) {
        val viewModel = TestViewModel()

        val effects = mutableListOf<TestEffect>()
        val collectJob = launch {
            viewModel.sideEffect.collect { effects.add(it) }
        }

        viewModel.emitEffect()
        testDispatcher.scheduler.advanceUntilIdle()
        collectJob.cancel()

        assertEquals(1, effects.size)
        assertEquals(TestEffect.ShowToast, effects.first())
    }

    @Test
    fun `handler should allow accessing current state`() = runTest(testDispatcher) {
        val viewModel = TestViewModel(initialState = TestState(count = 5))

        viewModel.accessState()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(15, viewModel.state.value.count)
    }

    @Test
    fun `SavedStateHandle should persist and restore state`() = runTest(testDispatcher) {
        val savedStateHandle = SavedStateHandle()

        // Create first viewModel and update state
        val viewModel1 = TestViewModel(
            initialState = TestState(count = 0),
            savedStateHandle = savedStateHandle
        )
        viewModel1.increment()
        viewModel1.increment()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, viewModel1.state.value.count)
        assertEquals(TestState(count = 2), savedStateHandle.get<TestState>("mvi_state"))

        // Create second viewModel with same SavedStateHandle - should restore state
        val viewModel2 = TestViewModel(
            initialState = TestState(count = 0),
            savedStateHandle = savedStateHandle
        )

        assertEquals(2, viewModel2.state.value.count)
    }

    @Test
    fun `SavedStateHandle requires Parcelable state`() = runTest(testDispatcher) {
        data class NonParcelableState(val count: Int = 0) : ViewState

        class NonParcelableViewModel : MviViewModel<NonParcelableState, TestIntent, TestEffect>(
            initialState = NonParcelableState(),
            savedStateHandle = SavedStateHandle()
        ) {
            override val dispatcher = testDispatcher
        }

        val viewModel = NonParcelableViewModel()

        // Exception should be thrown when accessing the container (lazy initialization)
        assertFailsWith<IllegalArgumentException> {
            viewModel.state
        }
    }

    @Test
    fun `debugMode should work without errors`() = runTest(testDispatcher) {
        val viewModel = TestViewModel(debugMode = true)

        viewModel.increment()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.state.value.count)
    }

    @Test
    fun `state flow should emit updates`() = runTest(testDispatcher) {
        val viewModel = TestViewModel()
        val states = mutableListOf<TestState>()

        val collectJob = launch {
            viewModel.state.collect { states.add(it) }
        }

        viewModel.increment()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.increment()
        testDispatcher.scheduler.advanceUntilIdle()

        collectJob.cancel()

        // Initial state + 2 updates
        assertEquals(3, states.size)
        assertEquals(0, states[0].count)
        assertEquals(1, states[1].count)
        assertEquals(2, states[2].count)
    }

    @Test
    fun `concurrent handler calls should be thread-safe`() = runTest(testDispatcher) {
        val viewModel = TestViewModel()

        repeat(100) {
            viewModel.increment()
        }

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(100, viewModel.state.value.count)
    }
}
