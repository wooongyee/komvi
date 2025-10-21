package io.github.wooongyee.komvi.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Internal implementation of [MviContainer].
 *
 * Provides thread-safe state management using [MutableStateFlow]
 * and side effect handling using [MutableSharedFlow].
 *
 * @param initialState The initial state value
 * @param scope The [CoroutineScope] for launching coroutines (typically viewModelScope)
 * @param S The type of [ViewState]
 * @param E The type of [SideEffect]
 */
internal class MviContainerImpl<S : ViewState, E : SideEffect>(
    initialState: S,
    private val scope: CoroutineScope
) : MviContainer<S, E> {

    private val _state = MutableStateFlow(initialState)
    override val state: StateFlow<S> = _state.asStateFlow()

    private val _sideEffect = MutableSharedFlow<E>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val sideEffect = _sideEffect.asSharedFlow()

    override fun intent(block: suspend IntentScope<S, E>.() -> Unit) {
        scope.launch {
            IntentScope(this@MviContainerImpl).block()
        }
    }

    /**
     * Updates state in a thread-safe manner.
     * Uses [MutableStateFlow.update] to ensure atomicity.
     *
     * @param reducer Function that transforms current state to new state
     */
    internal suspend fun updateState(reducer: S.() -> S) {
        _state.update { currentState ->
            currentState.reducer()
        }
    }

    /**
     * Emits a side effect to all active collectors.
     *
     * @param effect The side effect to emit
     */
    internal suspend fun emitSideEffect(effect: E) {
        _sideEffect.emit(effect)
    }
}
