package io.github.wooongyee.komvi.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * MVI Container for managing state and side effects.
 *
 * This is the core component of the MVI pattern that:
 * - Holds the current UI state as a [StateFlow]
 * - Emits one-time side effects as a [Flow]
 * - Processes intents through the [intent] function
 *
 * @param S The type of [ViewState]
 * @param I The type of [Intent]
 * @param E The type of [SideEffect]
 *
 * Example:
 * ```
 * class LoginViewModel : ViewModel() {
 *     private val container = container<LoginState, LoginIntent, LoginEffect>(
 *         initialState = LoginState(),
 *         scope = viewModelScope
 *     )
 *
 *     val state: StateFlow<LoginState> = container.state
 *     val sideEffect: Flow<LoginEffect> = container.sideEffect
 *
 *     fun onLoginClick() = container.intent {
 *         reduce { copy(isLoading = true) }
 *         // ... perform login
 *     }
 * }
 * ```
 */
interface MviContainer<S : ViewState, I : Intent, E : SideEffect> {

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

    /**
     * Executes an intent block within [IntentScope].
     *
     * Inside the block, you can:
     * - Access current state via [IntentScope.state]
     * - Update state via [IntentScope.reduce]
     * - Post side effects via [IntentScope.postSideEffect]
     *
     * @param block The intent processing block with [IntentScope] as receiver
     *
     * Example:
     * ```
     * container.intent {
     *     reduce { copy(email = "test@example.com") }
     *     postSideEffect(LoginEffect.ShowToast("Email updated"))
     * }
     * ```
     */
    fun intent(block: suspend IntentScope<S, I, E>.() -> Unit)
}

/**
 * Creates a new [MviContainer] instance.
 *
 * @param initialState The initial state value
 * @param scope The [CoroutineScope] for launching coroutines
 * @param debugMode Enable debug logging for state changes (default: false)
 * @param S The type of [ViewState]
 * @param I The type of [Intent]
 * @param E The type of [SideEffect]
 * @return A new [MviContainer] instance
 */
fun <S : ViewState, I : Intent, E : SideEffect> container(
    initialState: S,
    scope: CoroutineScope,
    debugMode: Boolean = false
): MviContainer<S, I, E> {
    return MviContainerImpl(initialState, scope, debugMode)
}
