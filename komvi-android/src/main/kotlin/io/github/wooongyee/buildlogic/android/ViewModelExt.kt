package io.github.wooongyee.buildlogic.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.wooongyee.komvi.core.MviContainer
import io.github.wooongyee.komvi.core.SideEffect
import io.github.wooongyee.komvi.core.ViewState
import io.github.wooongyee.komvi.core.container

/**
 * Creates an MVI container using the ViewModel's viewModelScope.
 *
 * This extension function allows creating an MVI container that is automatically
 * tied to the ViewModel's lifecycle, ensuring proper cleanup when the ViewModel is cleared.
 *
 * @param S The type of view state that extends [ViewState]
 * @param E The type of side effect that extends [SideEffect]
 * @param initialState The initial state of the view
 * @return A new [MviContainer] instance scoped to this ViewModel
 */
fun <S : ViewState, E : SideEffect> ViewModel.container(
    initialState: S
): MviContainer<S, E> = container(
    initialState = initialState,
    scope = viewModelScope
)
