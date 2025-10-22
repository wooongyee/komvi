package io.github.wooongyee.komvi.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.wooongyee.komvi.core.MviContainer
import io.github.wooongyee.komvi.core.MviContainerHost
import io.github.wooongyee.komvi.core.SideEffect
import io.github.wooongyee.komvi.core.ViewState
import io.github.wooongyee.komvi.core.container

/**
 * Base ViewModel class that integrates MVI pattern with Android's ViewModel lifecycle.
 *
 * This abstract class provides MVI container functionality tied to the ViewModel's lifecycle scope.
 * The container is automatically cleaned up when the ViewModel is cleared.
 *
 * @param S The type of view state that extends [ViewState]
 * @param E The type of side effect that extends [SideEffect]
 * @param initialState The initial state of the view
 */
abstract class MviViewModel<S : ViewState, E : SideEffect>(
    initialState: S
) : ViewModel(), MviContainerHost<S, E> {

    override val container: MviContainer<S, E> = container(
        initialState = initialState,
        scope = viewModelScope
    )
}
