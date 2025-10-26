package io.github.wooongyee.komvi.android

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.wooongyee.komvi.core.Intent
import io.github.wooongyee.komvi.core.MviContainer
import io.github.wooongyee.komvi.core.SideEffect
import io.github.wooongyee.komvi.core.ViewState
import io.github.wooongyee.komvi.core.container
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Creates an MVI container with SavedStateHandle support for state persistence.
 *
 * This extension function automatically restores state from SavedStateHandle
 * and saves state changes back to it, surviving process death.
 *
 * Note: The state must be Parcelable. Use @Parcelize annotation for easy implementation.
 *
 * Example:
 * ```
 * @Parcelize
 * data class MyState(...) : ViewState, Parcelable
 *
 * class MyViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
 *     private val container = containerWithSavedState(
 *         initialState = MyState(),
 *         savedStateHandle = savedStateHandle
 *     )
 * }
 * ```
 *
 * @param S The type of view state that extends [ViewState] and [Parcelable]
 * @param I The type of intent that extends [Intent]
 * @param E The type of side effect that extends [SideEffect]
 * @param initialState The initial state if no saved state exists
 * @param savedStateHandle SavedStateHandle for state persistence
 * @param stateKey Key for saving state (default: "mvi_state")
 * @param debugMode Enable debug logging (default: false)
 * @param dispatcher Coroutine dispatcher for intents (default: Dispatchers.Default)
 * @return A new [MviContainer] instance with state persistence
 */
fun <S, I : Intent, E : SideEffect> ViewModel.containerWithSavedState(
    initialState: S,
    savedStateHandle: SavedStateHandle,
    stateKey: String = "mvi_state",
    debugMode: Boolean = false,
    dispatcher: CoroutineDispatcher = Dispatchers.Default
): MviContainer<S, I, E> where S : ViewState, S : Parcelable {
    val container = container<S, I, E>(
        initialState = savedStateHandle.get<S>(stateKey) ?: initialState,
        scope = viewModelScope,
        debugMode = debugMode,
        dispatcher = dispatcher
    )

    // Automatically save state changes
    viewModelScope.launch {
        container.state.collect { state ->
            savedStateHandle[stateKey] = state
        }
    }

    return container
}
