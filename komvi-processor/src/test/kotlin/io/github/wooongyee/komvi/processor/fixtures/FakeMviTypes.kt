package io.github.wooongyee.komvi.processor.fixtures

import io.github.wooongyee.komvi.core.Intent
import io.github.wooongyee.komvi.core.IntentScope
import io.github.wooongyee.komvi.core.MviViewModelMarker
import io.github.wooongyee.komvi.core.SideEffect
import io.github.wooongyee.komvi.core.ViewState

/**
 * Fake ViewState for testing without Android dependencies
 */
data class FakeViewState(
    val count: Int = 0,
    val loading: Boolean = false
) : ViewState

/**
 * Fake SideEffect for testing without Android dependencies
 */
sealed interface FakeSideEffect : SideEffect {
    data class ShowToast(val message: String) : FakeSideEffect
    data object NavigateToHome : FakeSideEffect
}

/**
 * Fake Intent for testing without Android dependencies
 */
sealed interface FakeIntent : Intent {
    sealed interface ViewAction : FakeIntent {
        data object Increment : ViewAction
        data object Decrement : ViewAction
        data class SetValue(val value: Int) : ViewAction
    }

    sealed interface Internal : FakeIntent {
        data object OnLoadComplete : Internal
        data class OnError(val error: String) : Internal
    }
}

/**
 * Fake MviViewModel for testing without Android dependencies.
 * Implements MviViewModelMarker so the processor can recognize it.
 */
abstract class FakeMviViewModel<S : ViewState, I : Intent, E : SideEffect> : MviViewModelMarker {

    /**
     * DSL helper function for defining intent handlers (mimics the real MviViewModel).
     */
    protected fun handler(block: suspend IntentScope<S, I, E>.() -> Unit): suspend IntentScope<S, I, E>.() -> Unit = block

    /**
     * Executes an intent handler block (mimics the real MviViewModel).
     * This is called by KSP-generated dispatch functions.
     */
    fun executeHandler(
        block: suspend IntentScope<S, I, E>.() -> Unit,
        executionMode: String = "PARALLEL",
        handlerKey: String
    ) {
        // No-op for compile testing
    }
}
