package io.github.wooongyee.komvi.core

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * MVI Container for managing state and side effects.
 *
 * This is the core component of the MVI pattern that:
 * - Holds the current UI state as a [StateFlow]
 * - Emits one-time side effects as a [Flow]
 * - Processes intents internally (not directly accessible)
 *
 * This is a sealed interface to enforce that state changes can only happen
 * through intentScope in MviViewModel, preventing direct manipulation from
 * the View layer.
 *
 * @param S The type of [ViewState]
 * @param I The type of [Intent]
 * @param E The type of [SideEffect]
 */
sealed interface MviContainer<S : ViewState, I : Intent, E : SideEffect> {

    /**
     * Current state as a hot [StateFlow].
     * Collectors immediately receive the latest state upon collection.
     */
    val state: StateFlow<S>

    /**
     * Side effects as a hot [Flow].
     * Represents one-time events that should be consumed only once.
     */
    val sideEffect: Flow<E>
}

/**
 * Creates a new [MviContainer] instance.
 *
 * @param initialState The initial state value
 * @param scope The [CoroutineScope] for launching coroutines
 * @param debugMode Enable debug logging for state changes (default: false)
 * @param dispatcher The [CoroutineDispatcher] for executing intents (default: Dispatchers.Default)
 * @param S The type of [ViewState]
 * @param I The type of [Intent]
 * @param E The type of [SideEffect]
 * @return A new [MviContainer] instance
 */
fun <S : ViewState, I : Intent, E : SideEffect> container(
    initialState: S,
    scope: CoroutineScope,
    debugMode: Boolean = false,
    dispatcher: CoroutineDispatcher = Dispatchers.Default
): MviContainer<S, I, E> {
    return MviContainerImpl(initialState, scope, debugMode, dispatcher)
}

/**
 * Executes an intent block on the container.
 * This is a public inline function that allows MviViewModel to call the internal intent() function.
 *
 * **For internal use only.** Should only be called from MviViewModel.intentScope.
 *
 * @param container The MviContainer instance
 * @param block The intent processing block with [IntentScope] as receiver
 */
inline fun <S : ViewState, I : Intent, E : SideEffect> executeContainerIntent(
    container: MviContainer<S, I, E>,
    noinline block: suspend IntentScope<S, I, E>.() -> Unit
) {
    (container as MviContainerImpl).intent(block)
}
