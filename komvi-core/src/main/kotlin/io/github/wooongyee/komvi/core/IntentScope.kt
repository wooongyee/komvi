package io.github.wooongyee.komvi.core

/**
 * DSL scope for processing intents within [MviContainer].
 *
 * Provides functions to:
 * - Access current state via [state] property
 * - Update state via [reduce] function
 * - Emit side effects via [postSideEffect] function
 *
 * This scope is the receiver of the lambda passed to [MviContainer.intent].
 *
 * @param S The type of [ViewState]
 * @param I The type of [Intent]
 * @param E The type of [SideEffect]
 */
class IntentScope<S : ViewState, I : Intent, E : SideEffect> internal constructor(
    private val container: MviContainerImpl<S, I, E>
) {

    /**
     * Current state.
     * Always returns the latest state value.
     */
    val state: S
        get() = container.state.value

    /**
     * Updates the state by applying the [reducer] function.
     *
     * The [reducer] receives the current state and returns a new state.
     * State updates are thread-safe.
     *
     * @param reducer A function that takes current state and returns new state
     */
    suspend fun reduce(reducer: S.() -> S) {
        if (container.debugMode) {
            val oldState = container.state.value
            val newState = oldState.reducer()
            val stateTag = oldState::class.simpleName ?: "ViewState"
            println("[$stateTag] State changed: $oldState -> $newState")
            container.updateState { newState }
        } else {
            container.updateState(reducer)
        }
    }

    /**
     * Emits a one-time side effect.
     *
     * Side effects are consumed only once by collectors.
     * Multiple calls will emit multiple effects.
     *
     * @param effect The side effect to emit
     */
    suspend fun postSideEffect(effect: E) {
        container.emitSideEffect(effect)
    }
}
