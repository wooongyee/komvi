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
     * Protected to enforce MVI pattern - state changes must go through Intent handlers only.
     */
    val container: MviContainer<S, I, E>
        get() = throw UnsupportedOperationException("Direct container access is not allowed. Use intentScope in Intent handlers.")

    /**
     * Shortcut property for accessing current state.
     */
    val state: StateFlow<S>

    /**
     * Shortcut property for accessing side effects.
     */
    val sideEffect: Flow<E>

    /**
     * Creates an Intent scope for processing intents.
     * This should only be called from Intent handler functions.
     *
     * @param block The intent processing block with [IntentScope] as receiver
     */
    fun intentScope(block: suspend IntentScope<S, I, E>.() -> Unit)
}
