package io.github.wooongyee.komvi.android

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.wooongyee.komvi.core.Intent
import io.github.wooongyee.komvi.core.IntentScope
import io.github.wooongyee.komvi.core.MviContainer
import io.github.wooongyee.komvi.core.MviContainerHost
import io.github.wooongyee.komvi.core.SideEffect
import io.github.wooongyee.komvi.core.ViewState
import io.github.wooongyee.komvi.core.container
import io.github.wooongyee.komvi.core.executeContainerIntent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Base ViewModel class that integrates MVI pattern with Android's ViewModel lifecycle.
 *
 * This abstract class provides MVI container functionality tied to the ViewModel's lifecycle scope.
 * The container is automatically cleaned up when the ViewModel is cleared.
 *
 * State changes can only happen through [intentScope] which is protected, preventing
 * direct manipulation from the View layer and enforcing the MVI pattern.
 *
 * Note: If using savedStateHandle, the state (S) must implement Parcelable.
 * Use @Parcelize annotation for easy implementation.
 *
 * @param S The type of view state that extends [ViewState]
 * @param I The type of intent that extends [Intent]
 * @param E The type of side effect that extends [SideEffect]
 * @param initialState The initial state of the view
 * @param savedStateHandle Optional SavedStateHandle for state persistence (requires S to be Parcelable)
 * @param stateKey Key for saving state in SavedStateHandle (default: "mvi_state")
 * @param debugMode Enable debug logging for state changes (default: false, use BuildConfig.DEBUG in production)
 * @param dispatcher The coroutine dispatcher for executing intents (default: Dispatchers.Default)
 */
abstract class MviViewModel<S : ViewState, I : Intent, E : SideEffect>(
    initialState: S,
    savedStateHandle: SavedStateHandle? = null,
    stateKey: String = "mvi_state",
    debugMode: Boolean = false,
    dispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel(), MviContainerHost<S, I, E> {

    @PublishedApi
    internal val _container: MviContainer<S, I, E> = container(
        initialState = savedStateHandle?.get<S>(stateKey) ?: initialState,
        scope = viewModelScope,
        debugMode = debugMode,
        dispatcher = dispatcher
    )

    init {
        // Automatically save state when it changes (requires S to be Parcelable)
        savedStateHandle?.let { handle ->
            require(initialState is Parcelable) {
                "State must implement Parcelable when using SavedStateHandle. " +
                "Use @Parcelize annotation on your ViewState data class."
            }

            viewModelScope.launch {
                _container.state.collect { state ->
                    handle[stateKey] = state
                }
            }
        }
    }

    override val state: StateFlow<S> get() = _container.state
    override val sideEffect: Flow<E> get() = _container.sideEffect

    /**
     * Creates an Intent scope for processing intents.
     * This function is protected to prevent direct calls from the View layer.
     * Only ViewModel internal functions should use this.
     *
     * @param block The intent processing block with [IntentScope] as receiver
     */
    protected inline fun intentScope(noinline block: suspend IntentScope<S, I, E>.() -> Unit) {
        executeContainerIntent(_container, block)
    }
}
