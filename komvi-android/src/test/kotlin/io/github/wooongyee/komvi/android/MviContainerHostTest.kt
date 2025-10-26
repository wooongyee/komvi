package io.github.wooongyee.komvi.android

import io.github.wooongyee.komvi.core.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MviContainerHostTest {

    // Test state, intent, and effect
    data class TestState(val count: Int = 0) : ViewState

    sealed interface TestIntent : Intent {
        sealed interface ViewAction : TestIntent {
            data object Increment : ViewAction
        }

        sealed interface Internal : TestIntent {
            data object OnSuccess : Internal
        }
    }

    sealed interface TestEffect : SideEffect {
        data object ShowToast : TestEffect
    }

    class TestHost(
        scope: CoroutineScope
    ) : MviContainerHost<TestState, TestIntent, TestEffect> {
        private val _container: MviContainer<TestState, TestIntent, TestEffect> = container(
            initialState = TestState(),
            scope = scope,
            debugMode = false
        )

        override val container: MviContainer<TestState, TestIntent, TestEffect>
            get() = throw UnsupportedOperationException("Direct container access is not allowed. Use intentScope in Intent handlers.")

        override val state: StateFlow<TestState> get() = _container.state
        override val sideEffect: Flow<TestEffect> get() = _container.sideEffect

        override fun intentScope(block: suspend IntentScope<TestState, TestIntent, TestEffect>.() -> Unit) {
            _container.intent(block)
        }

        fun increment() = intentScope {
            reduce { copy(count = count + 1) }
        }

        fun emitEffect() = intentScope {
            postSideEffect(TestEffect.ShowToast)
        }

        fun accessCurrentState(): Int = state.value.count
    }

    @Test
    fun `direct container access should throw exception`() = runTest {
        val host = TestHost(this)

        val exception = assertFailsWith<UnsupportedOperationException> {
            host.container
        }

        assertEquals(
            "Direct container access is not allowed. Use intentScope in Intent handlers.",
            exception.message
        )
    }

    @Test
    fun `state should be accessible through MviContainerHost`() = runTest {
        val host = TestHost(this)

        assertEquals(0, host.state.value.count)
    }

    @Test
    fun `intentScope should allow state changes through reduce`() = runTest {
        val host = TestHost(this)

        host.increment()
        testScheduler.advanceUntilIdle()

        assertEquals(1, host.state.value.count)
    }

    @Test
    fun `intentScope should allow accessing state`() = runTest {
        val host = TestHost(this)

        host.increment()
        testScheduler.advanceUntilIdle()

        assertEquals(1, host.accessCurrentState())
    }

    @Test
    fun `MVI pattern enforcement - state changes only through intentScope`() = runTest {
        val host = TestHost(this)

        // This should work: state change through intentScope
        host.intentScope {
            reduce { copy(count = 42) }
        }
        testScheduler.advanceUntilIdle()

        assertEquals(42, host.state.value.count)

        // This should fail: trying to access container directly
        assertFailsWith<UnsupportedOperationException> {
            host.container.intent {
                reduce { copy(count = 100) }
            }
        }
    }
}
