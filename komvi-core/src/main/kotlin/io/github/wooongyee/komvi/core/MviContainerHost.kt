package io.github.wooongyee.komvi.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface for classes that host an [MviContainer].
 *
 * Typically implemented by ViewModel classes to provide convenient
 * access to state and side effects.
 *
 * @param S The type of [ViewState]
 * @param I The type of [Intent]
 * @param E The type of [SideEffect]
 */
interface MviContainerHost<S : ViewState, I : Intent, E : SideEffect> {

    /**
     * The MVI container instance.
     */
    val container: MviContainer<S, I, E>

    /**
     * Shortcut property for accessing current state.
     * Delegates to [MviContainer.state].
     */
    val state: StateFlow<S>
        get() = container.state

    /**
     * Shortcut property for accessing side effects.
     * Delegates to [MviContainer.sideEffect].
     */
    val sideEffect: Flow<E>
        get() = container.sideEffect

    /**
     * Executes an intent block.
     * Delegates to [MviContainer.intent].
     *
     * @param block The intent processing block with [IntentScope] as receiver
     */
    fun intent(block: suspend IntentScope<S, I, E>.() -> Unit) {
        container.intent(block)
    }
}
